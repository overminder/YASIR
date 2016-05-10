from .oop import W_Fixnum


class Expr(object):
    def to_repr(self):
        return '#<Expr>'

    def evaluate(self, env, cont):
        raise NotImplementedError('Expr.evaluate: abstract method')


class Const(Expr):
    def __init__(self, w_value):
        self._w_value = w_value

    def evaluate(self, env, cont):
        return cont.cont(self._w_value)


class Add(Expr):
    def __init__(self, lhs, rhs):
        self._lhs = lhs
        self._rhs = rhs

    def to_repr(self):
        return '#<Add %s %s>' % (self.lhs.to_repr(), self.rhs.to_repr())

    def evaluate(self, env, cont):
        return self._lhs, env, AddCont(self._rhs, env, cont)


class Cont(object):
    def to_repr(self):
        return '#<Cont>'

    def cont(self, w_value):
        raise NotImplementedError('Cont.cont: abstract method')


class AddCont(Cont):
    def __init__(self, rhs, env, cont):
        self._rhs = rhs
        self._env = env
        self._cont = cont

    def to_repr(self):
        return '#<AddCont %s>' % self._rhs.to_repr()

    def cont(self, w_value):
        return self._rhs, self._env, AddCont2(w_value, self._cont)


class AddCont2(Cont):
    def __init__(self, w_value, cont):
        assert isinstance(w_value, W_Fixnum)
        assert isinstance(cont, Cont)
        self._lhs_ival = w_value.ival()
        self._cont = cont

    def to_repr(self):
        return '#<AddCont2 %s>' % self._lhs_ival

    def cont(self, w_value):
        assert isinstance(w_value, W_Fixnum)
        res = self._lhs_ival + w_value.ival()
        return self._cont.cont(W_Fixnum(res))


class Halt(Cont):
    def to_repr(self):
        return '#<Halt>'

    def cont(self, w_value):
        from .interp import HaltException
        raise HaltException(w_value)
