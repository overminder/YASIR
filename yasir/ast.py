import inspect

from rpython.rlib import jit
from rpython.rlib.unroll import unrolling_iterable

from . import oop, pretty
from .rt import Env, EnvTypeMismatch

class Expr(pretty.PrettyBase):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#<Expr>')

    def evaluate(self, env, cont):
        assert isinstance(cont, Cont)
        raise NotImplementedError('Expr.evaluate: abstract method')


class Cont(pretty.PrettyBase):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#<Cont>')

    def cont(self, w_value, env):
        if isinstance(w_value, oop.W_Fixnum):
            return self.cont_fixnum(w_value.ival(), env)
        if isinstance(w_value, oop.W_Bool):
            return self.cont_bool(w_value.to_bool(), env)
        else:
            return self.cont_any(w_value, env)

    def cont_fixnum(self, value, env):
        return self.cont_any(oop.W_Fixnum(value), env)

    def cont_bool(self, value, env):
        return self.cont_any(oop.W_Bool.wrap(value), env)

    def cont_any(self, w_value, env):
        assert isinstance(w_value, oop.W_Value)
        raise NotImplementedError('Cont.cont: abstract method')

class LambdaInfo(Expr):
    _immutable_ = True

    def __init__(self, name, arity, extra_frame_slots, body):
        assert arity >= 0
        assert extra_frame_slots >= 0

        self._name = name
        self._arity = arity
        self._frame_size = arity + extra_frame_slots
        self._body = body

    def to_pretty(self):
        return pretty.atom('#lambda-info').append(self._name)

    def name(self):
        return self._name

    @jit.unroll_safe
    def build_expr_and_env(self, w_argvalues, env):
        from .rt import Env

        arity = self._arity
        assert len(w_argvalues) == arity
        frame = [None] * self._frame_size
        for i in range(arity):
            frame[i] = w_argvalues[i]
        for i in range(self._frame_size - arity):
            frame[arity + i] = oop.W_Box(oop.w_undef)

        return self._body, Env.extend(env, frame)

    def evaluate(self, env, cont):
        return cont.cont(oop.W_Lambda(self, env), env)


class Apply(Expr):
    _immutable_ = True

    def __init__(self, func, args):
        self._func = func
        self._args = args

    def to_pretty(self):
        return pretty.atom('#apply').append(self._func).extend(self._args)

    def evaluate(self, env, cont):
        return self._func, env, ApplyCont(self._args, env, cont, [])


class ApplyCont(Cont):
    _immutable_ = True

    def __init__(self, args, orig_env, cont, w_values, w_funcval=None):
        self._args = args
        self._orig_env = orig_env
        self._cont = cont
        self._w_values = w_values
        self._w_funcval = w_funcval

    def to_pretty(self):
        return pretty.atom('#apply-cont').append(self._w_funcval).extend(
            self._w_values)

    def cont_any(self, w_value, env):
        args = self._args
        w_values = self._w_values
        w_funcval = self._w_funcval
        next_arg_ix = len(w_values)
        if w_funcval is None:
            assert isinstance(w_value, oop.W_Lambda)
            w_funcval = w_value
        else:
            assert len(args) > next_arg_ix
            w_more = w_values + [w_value]
            w_values = list(w_more)
            next_arg_ix += 1

        if len(args) == next_arg_ix:
            # Fully saturated: enter.
            return w_funcval.call(w_values, ReturnCont(self._orig_env,
                                                       self._cont))
        else:
            cont = ApplyCont(args, self._orig_env, self._cont, w_values,
                             w_funcval)
            return args[next_arg_ix], env, cont


# That restores the caller's env and cont.
class ReturnCont(Cont):
    _immutable_ = True

    def __init__(self, env, cont):
        self._env = env
        self._cont = cont

    def to_pretty(self):
        return pretty.atom('#return-cont')

    def cont_fixnum(self, value, env_unused):
        return self._cont.cont_fixnum(value, self._env)

    def cont_any(self, w_value, env_unused):
        return self._cont.cont(w_value, self._env)


'''
class DefineVar(Expr):
    _immutable_ = True

    def __init__(self, ix, expr):
        assert isinstance(expr, Expr)

        self._ix = ix
        self._expr = expr

    def to_pretty(self):
        return pretty.atom('#define').append_kw('ix', self._ix).append(self._expr)

    def evaluate(self, env, cont):
        return self._expr, env, DefineVarCont(self._ix, cont)


class DefineVarCont(Cont):
    _immutable_ = True

    def __init__(self, ix, cont):
        assert isinstance(cont, Cont)

        self._ix = ix
        self._cont = cont

    def to_pretty(self):
        return pretty.atom('#define-cont').append(self._ix)

    def cont(self, w_value, env):
        env.set(w_value, self._ix)
        return self._cont.cont(oop.w_nil, env)
'''

class ReadVar(Expr):
    _immutable_ = True

    def __init__(self, ix, depth=0):
        self._ix = ix
        self._depth = depth

    def to_pretty(self):
        return pretty.atom('#readvar').append_kw('ix', self._ix)\
                                      .append_kw('depth', self._depth)

    def evaluate(self, env, cont):
        #print('ReadVar %s on %s' % (self._w_sym.name(), env))
        try:
            res = env.get_fixnum(self._ix, self._depth)
            return cont.cont_fixnum(res, env)
        except EnvTypeMismatch as e:
            w_res = env.get(self._ix, self._depth)
            return cont.cont(w_res, env)


class Seq(Expr):
    _immutable_ = True

    def __init__(self, exprs):
        self._exprs = exprs

    def to_pretty(self):
        return pretty.atom('#seq').extend(self._exprs)

    def evaluate(self, env, cont):
        es = self._exprs
        nes = len(es)
        if nes == 0:
            # Zero exprs
            return cont.cont(oop.w_nil, env)

        e = es[0]
        if nes == 1:
            # One expr.
            return e, env, cont
        else:
            return e, env, SeqCont(1, es, cont)


class SeqCont(Cont):
    _immutable_ = True

    def __init__(self, ix, es, cont):
        self._ix = ix
        self._es = es
        self._cont = cont

    def to_pretty(self):
        return pretty.atom('#seq-cont').append_kw('ix',
                                                  self._ix).extend(self._es)

    def cont_any(self, w_value, env):
        if self._ix == len(self._es):
            return self._cont.cont(w_value, env)
        else:
            return self._es[self._ix], env, SeqCont(self._ix + 1, self._es,
                                                    self._cont)


class Const(Expr):
    _immutable_ = True

    def __init__(self, w_value):
        self._w_value = w_value

    def to_pretty(self):
        return pretty.atom('#const').append(self._w_value)

    def evaluate(self, env, cont):
        return cont.cont(self._w_value, env)

# Simplified and with specialized cont.
def make_simple_primop(name, func_w, argtypes=None):
    if inspect.isclass(func_w):
        real_func = func_w.__init__
        arity_modifier = -1
    else:
        real_func = func_w
        arity_modifier = 0
    arity = len(inspect.getargspec(real_func).args) + arity_modifier

    if argtypes is None:
        argtypes = [oop.W_Value] * arity

    arg_ixs = unrolling_iterable(range(arity))

    def expr_field_of(i):
        return '_e%d' % i

    def value_field_of(i):
        return '_v%d' % i

    expr_fields = list(enumerate(expr_field_of(i) for i in xrange(arity)))
    unrolling_expr_fields = unrolling_iterable(expr_fields)

    class PrimOp(Expr):
        _immutable_ = True

        def __init__(self, *exprs):
            assert len(exprs) == arity
            for i, expr_field in unrolling_expr_fields:
                setattr(self, expr_field, exprs[i])

        def to_pretty(self):
            res = pretty.atom('#%s' % name)
            for _, expr_field in unrolling_expr_fields:
                res = res.append(getattr(self, expr_field))
            return res

        def evaluate(self, env, cont):
            if arity == 0:
                return cont.cont(func_w(), env)
            else:
                _, first_expr_field = expr_fields[0]
                return getattr(self, first_expr_field), env, cont_classes[0](self, cont)

    PrimOp.__name__ = 'PrimOp%s' % name

    cont_classes = []

    def unwrap_fixnum(w_x):
        assert isinstance(w_x, oop.W_Fixnum)
        return w_x.ival()

    def make_cont_class(nth):
        value_fields = list(enumerate(value_field_of(i) for i in xrange(nth)))
        unrolling_value_fields = unrolling_iterable(value_fields)

        if argtypes[nth] is oop.W_Fixnum:
            cont_method_name = 'cont_fixnum'
        else:
            cont_method_name = 'cont'

        class PrimOpCont(Cont):
            _immutable_ = True

            def __init__(self, op, cont, *values):
                assert len(values) == nth
                self._op = op
                self._cont = cont
                for i, value_field in unrolling_value_fields:
                    setattr(self, value_field, values[i])

            def to_pretty(self):
                return pretty.atom('#%s-cont' % name)\
                                .append_kw('ix', i).append_kw('op', self._op)

        def cont_method(self, value, env):
            values = ()
            for _, value_field in unrolling_value_fields:
                values += (getattr(self, value_field),)
            values += (value,)
            if nth == arity - 1:
                # Saturated.
                return self._cont.cont(func_w(*values), env)
            else:
                mk_cont = cont_classes[nth + 1]
                cont = mk_cont(self._op, self._cont, *values)
                _, next_expr_field = expr_fields[nth + 1]
                return getattr(self._op, next_expr_field), env, cont

        setattr(PrimOpCont, cont_method_name, cont_method)

        PrimOpCont.__name__ = 'PrimOpCont%d%s' % (nth, name)
        return PrimOpCont

    for i in xrange(arity):
        cont_classes.append(make_cont_class(i))

    return PrimOp


def make_arith_binop(name, func, wrap_result=oop.W_Fixnum):
    def func_w(x, y):
        #print('Arith: %s %s %s' % (w_x, name, w_y))
        return wrap_result(func(x, y))

    return make_simple_primop(name, func_w, [oop.W_Fixnum] * 2)


Add = make_arith_binop('+', lambda x, y: x + y)
Sub = make_arith_binop('-', lambda x, y: x - y)
LessThan = make_arith_binop('<', lambda x, y: x < y, oop.W_Bool.wrap)

MkBox = make_simple_primop('MkBox', oop.W_Box)


def read_box_w(w_value):
    assert isinstance(w_value, oop.W_Box)
    return w_value.w_value()


ReadBox = make_simple_primop('ReadBox', read_box_w)


def write_box_w(w_box, w_value):
    assert isinstance(w_box, oop.W_Box)
    w_box.set_w(w_value)
    return oop.w_nil


WriteBox = make_simple_primop('WriteBox', write_box_w)


class If(Expr):
    _immutable_ = True

    def __init__(self, c, t, f):
        self._c = c
        self._t = t
        self._f = f

    def to_pretty(self):
        return pretty.atom('#if').append(self._c).append(self._t).append(
            self._f)

    def evaluate(self, env, cont):
        return self._c, env, IfCont(self._t, self._f, cont)


class IfCont(Cont):
    _immutable_ = True

    def __init__(self, t, f, cont):
        self._t = t
        self._f = f
        self._cont = cont

    def to_pretty(self):
        return pretty.atom('#if-cont').append(self._t).append(self._f)

    def cont_any(self, w_value, env):
        if w_value.to_bool():
            e = self._t
        else:
            e = self._f
        return e, env, self._cont


class Halt(Cont):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#halt')

    def cont_any(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)
