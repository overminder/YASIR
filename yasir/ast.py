from yasir import oop


class Expr(object):
    _immutable_ = True

    def to_repr(self):
        return '#<Expr>'

    def __repr__(self):
        return self.to_repr()

    def evaluate(self, env, cont):
        assert isinstance(cont, Cont)
        raise NotImplementedError('Expr.evaluate: abstract method')


class Cont(object):
    _immutable_ = True

    def to_repr(self):
        return '#<Cont>'

    def __repr__(self):
        return self.to_repr()

    def cont(self, w_value, env):
        assert isinstance(w_value, oop.W_Value)
        raise NotImplementedError('Cont.cont: abstract method')


class Lambda(Expr):
    _immutable_ = True

    def __init__(self, w_argnames, body):
        self._w_argnames = w_argnames
        self._body = body

    def to_repr(self):
        return '#<Lambda %s %s>' % (self._w_argnames, self._body)

    def evaluate(self, env, cont):
        return cont.cont(oop.W_Lambda(self._w_argnames, self._body, env), env)


class Apply(Expr):
    _immutable_ = True

    def __init__(self, func, args):
        self._func = func
        self._args = args

    def to_repr(self):
        return '#<Apply %s %s>' % (self._func.to_repr(), self._args)

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

    def to_repr(self):
        return '#<ApplyCont args=%s values=%s func=%s>' % (
            self._args, self._w_values, self._w_funcval)

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

    def cont(self, w_value, env_unused):
        return self._cont.cont(w_value, self._env)


class DefineVar(Expr):
    _immutable_ = True

    def __init__(self, w_sym, expr):
        assert isinstance(w_sym, oop.W_Symbol)
        assert isinstance(expr, Expr)

        self._w_sym = w_sym
        self._expr = expr

    def to_repr(self):
        return '#<DefineVar %s %s>' % (self._w_sym.to_repr(),
                                       self._expr.to_repr())

    def evaluate(self, env, cont):
        return self._expr, env, DefineVarCont(self._w_sym, cont)


class DefineVarCont(Cont):
    _immutable_ = True

    def __init__(self, w_sym, cont):
        assert isinstance(w_sym, oop.W_Symbol)
        assert isinstance(cont, Cont)

        self._w_sym = w_sym
        self._cont = cont

    def to_repr(self):
        return '#<DefineVarCont %s>' % self._w_sym

    def cont(self, w_value, env):
        return self._cont.cont(oop.w_nil, env.extend(self._w_sym, w_value))


class ReadVar(Expr):
    _immutable_ = True

    def __init__(self, w_sym):
        assert isinstance(w_sym, oop.W_Symbol)

        self._w_sym = w_sym

    def to_repr(self):
        return '#<ReadVar %s>' % (self._w_sym.to_repr(), )

    def evaluate(self, env, cont):
        #print('ReadVar %s on %s' % (self._w_sym.name(), env))
        from .interp import UndefinedVariable
        w_res = env.lookup(self._w_sym, oop.w_undef)
        if w_res is oop.w_undef:
            raise UndefinedVariable(self._w_sym)
        return cont.cont(w_res, env)


class Seq(Expr):
    _immutable_ = True

    def __init__(self, exprs):
        self._exprs = exprs

    def to_repr(self):
        return '#<Seq %s>' % (self._exprs, )

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

    def to_repr(self):
        return '#<SeqCont %s @%d>' % (self._es, self._ix)

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

    def to_repr(self):
        return '#<Const %s>' % self._w_value.to_repr()

    def evaluate(self, env, cont):
        return cont.cont(self._w_value, env)


def make_simple_primop(name, arity, func_w):
    class PrimOp(Expr):
        _immutable_ = True

        def __init__(self, *exprs):
            assert len(exprs) == arity
            self._exprs = list(exprs)

        def to_repr(self):
            return '#<PrimOp%s %s>' % (name, self._exprs)

        def evaluate(self, env, cont):
            if arity == 0:
                return cont.cont(func_w(), env)
            else:
                return self._exprs[0], env, PrimOpCont(
                    [None] * arity, 0, self._exprs, cont)

    PrimOp.__name__ = 'PrimOp%s' % name

    class PrimOpCont(Cont):
        _immutable_ = True

        def __init__(self, w_values, current_ix, exprs, cont):
            self._w_values = w_values
            self._current_ix = current_ix
            self._exprs = exprs
            self._cont = cont

        def to_repr(self):
            return '#<PrimOpCont%s %s @%d>' % (name, self._exprs,
                                               self._current_ix)

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

    PrimOpCont.__name__ = 'PRimOpCont_%s' % name

    d = {'func_w': func_w}
    src = '''
def call_func_w(w_values):
    return func_w(%s)
    ''' % ', '.join('w_values[%d]' % i for i in xrange(arity))

    exec src in d
    call_func_w = d['call_func_w']

    return PrimOp


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

    def to_repr(self):
        return '#<If>'

    def evaluate(self, env, cont):
        return self._c, env, IfCont(self._t, self._f, cont)


class IfCont(Cont):
    _immutable_ = True

    def __init__(self, t, f, cont):
        self._t = t
        self._f = f
        self._cont = cont

    def to_repr(self):
        return '#<IfCont>'

    def cont(self, w_value, env):
        if w_value.to_bool():
            e = self._t
        else:
            e = self._f
        return e, env, self._cont


class Halt(Cont):
    _immutable_ = True

    def to_repr(self):
        return '#<Halt>'

    def cont(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)
