from yasir import oop


class Expr(object):
    def to_repr(self):
        return '#<Expr>'

    def __repr__(self):
        return self.to_repr()

    def evaluate(self, env, cont):
        assert isinstance(cont, Cont)
        raise NotImplementedError('Expr.evaluate: abstract method')


class Cont(object):
    def to_repr(self):
        return '#<Cont>'

    def __repr__(self):
        return self.to_repr()

    def cont(self, w_value, env):
        assert isinstance(w_value, oop.W_Value)
        raise NotImplementedError('Cont.cont: abstract method')


class Lambda(Expr):
    def __init__(self, w_argnames, body):
        self._w_argnames = w_argnames
        self._body = body

    def to_repr(self):
        return '#<Lambda %s %s>' % (self._w_argnames, self._body)

    def evaluate(self, env, cont):
        return cont.cont(oop.W_Lambda(self._w_argnames, self._body, env), env)


class Apply(Expr):
    def __init__(self, func, args):
        self._func = func
        self._args = args

    def to_repr(self):
        return '#<Apply %s %s>' % (self._func.to_repr(), self._args.to_repr())

    def evaluate(self, env, cont):
        return self._func, env, ApplyCont(self._args, cont, [])


class ApplyCont(Cont):
    def __init__(self, args, cont, w_values, w_funcval=None):
        self._args = args
        self._cont = cont
        self._w_values = w_values
        self._w_funcval = w_funcval

    def to_repr(self):
        return '#<ApplyCont args=%s values=%s func=%s>' % (
            self._args, self._w_values, self._w_funcval)

    def cont(self, w_value, env):
        print self
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
            # Fully saturated.
            return w_funcval.call(w_values, self._cont)
        else:
            return args[next_arg_ix], env, ApplyCont(args, self._cont,
                                                     w_values, w_funcval)


class DefineVar(Expr):
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


class ReadVar(Expr):
    def __init__(self, w_sym):
        assert isinstance(w_sym, oop.W_Symbol)

        self._w_sym = w_sym

    def to_repr(self):
        return '#<ReadVar %s>' % (self._w_sym.to_repr(), )

    def evaluate(self, env, cont):
        from .interp import UndefinedVariable
        w_res = env.lookup(self._w_sym, oop.w_undef)
        if w_res is oop.w_undef:
            raise UndefinedVariable(self._w_sym)
        return cont.cont(w_res, env)

class MakeBox(Expr):
    def __init__(self, e):
        self._e = e

    def evaluate(self, env, cont):
        return self._e, env, BoxCont(cont)

class BoxCont(Cont):
    def __init__(self, cont):
        self._cont = Cont

    def cont(self, w_value, env):
        return self._cont.cont(oop.W_Box(w_value), env)

class Seq(Expr):
    def __init__(self, exprs):
        self._exprs = exprs

    def to_repr(self):
        return '#<Seq %s>' % (self._exprs,)

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
            return self._es[self._ix], env, SeqCont(self._ix + 1, self._es, self._cont)


class DefineVarCont(Cont):
    def __init__(self, w_sym, cont):
        assert isinstance(w_sym, oop.W_Symbol)
        assert isinstance(cont, Cont)

        self._w_sym = w_sym
        self._cont = cont

    def cont(self, w_value, env):
        return self._cont.cont(oop.w_nil, env.extend(self._w_sym, w_value))


class Const(Expr):
    def __init__(self, w_value):
        self._w_value = w_value

    def to_repr(self):
        return '#<Const %s>' % self._w_value.to_repr()

    def evaluate(self, env, cont):
        return cont.cont(self._w_value, env)


def make_simple_primop(name, arity, func_w):
    class PrimOp(Expr):
        def __init__(self, *exprs):
            assert len(exprs) == arity
            self._exprs = exprs

        def to_repr(self):
            return '#<PrimOp%s %s>' % (name, self._exprs)

        def evaluate(self, env, cont):
            if arity == 0:
                return cont.cont(func_w(), env)
            else:
                return self._exprs[0], env, PrimOpCont([None] * arity, 0, self._exprs, cont)

    PrimOp.__name__ = 'PrimOp%s' % name

    class PrimOpCont(Cont):
        def __init__(self, w_values, current_ix, exprs, cont):
            self._w_values = w_values
            self._current_ix = current_ix
            self._exprs = exprs
            self._cont = cont

        def to_repr(self):
            return '#<PrimOpCont%s %s>' % (name, self._exprs)

        def cont(self, w_value, env):
            # XXX: Mutation
            ix = self._current_ix
            self._w_values[ix] = w_value
            ix += 1
            if ix == arity:
                w_res = func_w(*self._w_values)
                return self._cont.cont(w_res, env)
            else:
                return self._exprs[ix], env, PrimOpCont(self._w_values, ix, self._exprs, self._cont)

    PrimOpCont.__name__ = 'PRimOpCont_%s' % name

    return PrimOp

def make_arith_binop(name, func, wrap_result=oop.W_Fixnum):
    def func_w(w_x, w_y):
        assert isinstance(w_x, oop.W_Fixnum)
        assert isinstance(w_y, oop.W_Fixnum)
        return wrap_result(func(w_x.ival(), w_y.ival()))
    return make_simple_primop(name, 2, func_w)

Add = make_arith_binop('+', lambda x, y: x + y)
Sub = make_arith_binop('-', lambda x, y: x - y)
LessThan = make_arith_binop('<', lambda x, y: x < y, oop.W_Bool.wrap)

class If(Expr):
    def __init__(self, c, t, f):
        self._c = c
        self._t = t
        self._f = f

    def to_repr(self):
        return '#<If>'

    def evaluate(self, env, cont):
        return self._c, env, IfCont(self._t, self._f, cont)


class IfCont(Cont):
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
    def to_repr(self):
        return '#<Halt>'

    def cont(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)