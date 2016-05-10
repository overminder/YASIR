from .oop import W_Value, W_Fixnum, W_Symbol, w_nil, w_undef


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
        assert isinstance(w_value, W_Value)
        raise NotImplementedError('Cont.cont: abstract method')


class Env(object):
    def __init__(self, w_sym, w_value, prev=None):
        self._w_sym = w_sym
        self._w_value = w_value
        self._prev = prev

    @staticmethod
    def lookup(thiz, w_sym, w_otherwise):
        while thiz is not None:
            assert isinstance(thiz, Env)
            if thiz._w_sym is w_sym:
                return thiz._w_value
            else:
                thiz = thiz._prev
        return w_otherwise

    @staticmethod
    def extend(thiz, w_sym, w_otherwise):
        return Env(w_sym, w_otherwise, thiz)


class DefineVar(Expr):
    def __init__(self, w_sym, expr):
        assert isinstance(w_sym, W_Symbol)
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
        assert isinstance(w_sym, W_Symbol)

        self._w_sym = w_sym

    def to_repr(self):
        return '#<ReadVar %s>' % (self._w_sym.to_repr(), )

    def evaluate(self, env, cont):
        from .interp import UndefinedVariable
        w_res = Env.lookup(env, self._w_sym, w_undef)
        if w_res is w_undef:
            raise UndefinedVariable(self._w_sym)
        cont.cont(w_res, env)


class Seq(Expr):
    def __init__(self, exprs, ix=0):
        self._exprs = exprs
        self._ix = ix

    def to_repr(self):
        return '#<Seq %s @%d>' % (self._exprs, self._ix)

    def evaluate(self, env, cont):
        es = self._exprs
        i = self._ix
        assert i < len(es)
        e = es[i]
        if i == len(es) - 1:
            # Last expr.
            return e, env, cont
        else:
            return e, env, SeqCont(Seq(es, i + 1), cont)


class SeqCont(Cont):
    def __init__(self, e, cont):
        self._e = e
        self._cont = cont

    def to_repr(self):
        return '#<SeqCont %s %s>' % (self._e.to_repr(), self._cont.to_repr())

    def cont(self, w_value, env):
        return self._e, env, self._cont


class DefineVarCont(Cont):
    def __init__(self, w_sym, cont):
        assert isinstance(w_sym, W_Symbol)
        assert isinstance(cont, Cont)

        self._w_sym = w_sym
        self._cont = cont

    def cont(self, w_value, env):
        return self._cont.cont(w_nil, Env.extend(env, self._w_sym, w_value))


class Const(Expr):
    def __init__(self, w_value):
        self._w_value = w_value

    def evaluate(self, env, cont):
        return cont.cont(self._w_value, env)


class Add(Expr):
    def __init__(self, lhs, rhs):
        self._lhs = lhs
        self._rhs = rhs

    def to_repr(self):
        return '#<Add %s %s>' % (self.lhs.to_repr(), self.rhs.to_repr())

    def evaluate(self, env, cont):
        return self._lhs, env, AddCont(self._rhs, cont)


# XXX: Generalize this.
class AddCont(Cont):
    def __init__(self, rhs, cont):
        self._rhs = rhs
        self._cont = cont

    def to_repr(self):
        return '#<AddCont %s>' % self._rhs.to_repr()

    def cont(self, w_value, env):
        return self._rhs, env, AddCont2(w_value, self._cont)


class AddCont2(Cont):
    def __init__(self, w_value, cont):
        assert isinstance(w_value, W_Fixnum)
        assert isinstance(cont, Cont)
        self._lhs_ival = w_value.ival()
        self._cont = cont

    def to_repr(self):
        return '#<AddCont2 %s>' % self._lhs_ival

    def cont(self, w_value, env):
        assert isinstance(w_value, W_Fixnum)
        res = self._lhs_ival + w_value.ival()
        return self._cont.cont(W_Fixnum(res), env)


class Halt(Cont):
    def to_repr(self):
        return '#<Halt>'

    def cont(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)
