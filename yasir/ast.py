from rpython.rlib import jit

from . import oop, pretty


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
        assert isinstance(w_value, oop.W_Value)
        raise NotImplementedError('Cont.cont: abstract method')


class LambdaInfo(Expr):
    _immutable_ = True

    def __init__(self, name, w_argnames, body):
        self._name = name
        self._w_argnames = w_argnames
        self._body = body

    def to_pretty(self):
        return pretty.atom('#lambda-info').append(self._name)

    def name(self):
        return self._name

    @jit.unroll_safe
    def build_expr_and_env(self, w_argvalues, env):
        from .rt import Env

        w_argnames = self._w_argnames
        assert len(w_argvalues) == len(w_argnames)
        for i in range(len(w_argvalues)):
            env = env.extend(w_argnames[i], w_argvalues[i])

        return self._body, env

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
        return pretty.atom('#apply-cont').append(self._w_funcval).extend(self._w_values)

    def cont(self, w_value, env):
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

    def cont(self, w_value, env_unused):
        return self._cont.cont(w_value, self._env)


class DefineVar(Expr):
    _immutable_ = True

    def __init__(self, w_sym, expr):
        assert isinstance(w_sym, oop.W_Symbol)
        assert isinstance(expr, Expr)

        self._w_sym = w_sym
        self._expr = expr

    def to_pretty(self):
        return pretty.atom('#define').append(self._w_sym).append(self._expr)

    def evaluate(self, env, cont):
        return self._expr, env, DefineVarCont(self._w_sym, cont)


class DefineVarCont(Cont):
    _immutable_ = True

    def __init__(self, w_sym, cont):
        assert isinstance(w_sym, oop.W_Symbol)
        assert isinstance(cont, Cont)

        self._w_sym = w_sym
        self._cont = cont

    def to_pretty(self):
        return pretty.atom('#define-cont').append(self._w_sym)

    def cont(self, w_value, env):
        return self._cont.cont(oop.w_nil, env.extend(self._w_sym, w_value))


class ReadVar(Expr):
    _immutable_ = True

    def __init__(self, w_sym):
        assert isinstance(w_sym, oop.W_Symbol)

        self._w_sym = w_sym

    def to_pretty(self):
        return pretty.atom('#readvar').append(self._w_sym)

    def evaluate(self, env, cont):
        #print('ReadVar %s on %s' % (self._w_sym.name(), env))
        w_res = env.lookup(self._w_sym)
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
        return pretty.atom('#seq-cont').append_kw('ix', self._ix).extend(self._es)

    def cont(self, w_value, env):
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


def make_simple_primop(name, arity, func_w):
    class PrimOp(Expr):
        _immutable_ = True

        def __init__(self, *exprs):
            assert len(exprs) == arity
            self._exprs = exprs

        def to_pretty(self):
            return pretty.atom('#%s' % name.lower()).extend(self._exprs)

        def evaluate(self, env, cont):
            if arity == 0:
                return cont.cont(func_w(), env)
            else:
                return self._exprs[0], env, PrimOpCont(
                    [None] * arity, 0, self._exprs, cont)

    PrimOp.__name__ = 'PrimOp%s' % name

    class PrimOpCont(Cont):
        _immutable_ = True
        _virtualizables_ = ['_w_values[*]', '_current_ix', '_exprs', 'cont']

        def __init__(self, w_values, current_ix, exprs, cont):
            self._w_values = w_values
            self._current_ix = current_ix
            self._exprs = exprs
            self._cont = cont

        def to_pretty(self):
            return pretty.atom('#%s-cont' % name.lower())\
                         .append_kw('ix', self._current_ix).extend(self._exprs)

        def cont(self, w_value, env):
            # XXX: Mutation
            ix = self._current_ix
            self._w_values[ix] = w_value
            ix += 1
            if ix == arity:
                w_res = call_func_w(self._w_values)
                return self._cont.cont(w_res, env)
            else:
                return self._exprs[ix], env, PrimOpCont(
                    self._w_values, ix, self._exprs, self._cont)

    PrimOpCont.__name__ = 'PrimOpCont_%s' % name

    d = {'func_w': func_w}
    src = '''
def call_func_w(w_values):
    return func_w(%s)
    ''' % ', '.join('w_values[%d]' % i for i in xrange(arity))

    exec src in d
    call_func_w = d['call_func_w']

    return PrimOp


# Specialize more.
def make_simple_primop_v2(name, arity, func_w):
    expr_init_args = ['e%d' % i for i in xrange(arity)]
    expr_fields = ['self._%s' % v for v in expr_init_args]
    exec_env = {
        'args': ', '.join(expr_init_args),
        'fields': ', '.join(expr_fields),
        'pretty_appends': ', '.join('.append(%s)' % f for f in expr_fields),
        'evaluate_to': 'cont.cont(func_w(), env)' if arity == 0 else \
          'self._e0, env, PrimOpCont0(%s, cont)' % ', '.join(expr_fields[1:])
    }
    exec_env.update(locals())
    expr_src = '''
def __init__(self, %(args)s):
    %(fields)s = %(args)s
def to_pretty(self):
    return pretty.atom('#%s' % name.lower())%(pretty_appends)s
def evaluate(self, env, cont):
    return %(evaluate_to)s
    ''' % exec_env

    exec expr_src in exec_env

    class PrimOp(Expr):
        _immutable_ = True

    for attr in  ['__init__', 'to_pretty', 'evaluate']:
        setattr(PrimOp, attr, exec_env[attr])

    PrimOp.__name__ = 'PrimOp%s' % name

    for i in xrange(arity):
        class PrimOpCont(Cont):
            _immutable_ = True

        cont_src = '''
def __init__(self, w_values, current_ix, exprs, cont):
    self._w_values = w_values
    self._current_ix = current_ix
    self._exprs = exprs
    self._cont = cont

        def to_pretty(self):
            return pretty.atom('#%s-cont' % name.lower())\
                         .append_kw('ix', self._current_ix).extend(self._exprs)

        def cont(self, w_value, env):
            # XXX: Mutation
            ix = self._current_ix
            self._w_values[ix] = w_value
            ix += 1
            if ix == arity:
                w_res = call_func_w(self._w_values)
                return self._cont.cont(w_res, env)
            else:
                return self._exprs[ix], env, PrimOpCont(
                    self._w_values, ix, self._exprs, self._cont)
'''
        if i == arity - 1:
            # Saturated.
            pass
        else:
            # Not yet.
            pass
        if i == 0:
            pass


def make_arith_binop(name, func, wrap_result=oop.W_Fixnum):
    def func_w(w_x, w_y):
        assert isinstance(w_x, oop.W_Fixnum)
        assert isinstance(w_y, oop.W_Fixnum)
        #print('Arith: %s %s %s' % (w_x, name, w_y))
        return wrap_result(func(w_x.ival(), w_y.ival()))

    return make_simple_primop(name, 2, func_w)


Add = make_arith_binop('+', lambda x, y: x + y)
Sub = make_arith_binop('-', lambda x, y: x - y)
LessThan = make_arith_binop('<', lambda x, y: x < y, oop.W_Bool.wrap)

MkBox = make_simple_primop('MkBox', 1, oop.W_Box)


def read_box_w(w_value):
    assert isinstance(w_value, oop.W_Box)
    return w_value.w_value()


ReadBox = make_simple_primop('ReadBox', 1, read_box_w)


def write_box_w(w_box, w_value):
    assert isinstance(w_box, oop.W_Box)
    w_box.set_w(w_value)
    return oop.w_nil


WriteBox = make_simple_primop('WriteBox', 2, write_box_w)


class If(Expr):
    _immutable_ = True

    def __init__(self, c, t, f):
        self._c = c
        self._t = t
        self._f = f

    def to_pretty(self):
        return pretty.atom('#if').append(self._c).append(self._t).append(self._f)

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

    def cont(self, w_value, env):
        if w_value.to_bool():
            e = self._t
        else:
            e = self._f
        return e, env, self._cont


class Halt(Cont):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#halt')

    def cont(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)
