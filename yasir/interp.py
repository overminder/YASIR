from .ast import Expr, Halt
from .oop import W_Value
from .rt import Env, nil_env

def interp(expr, env=nil_env):
    assert isinstance(expr, Expr)

    cont = Halt()
    try:
        while True:
            expr, env, cont = expr.evaluate(env, cont)
    except HaltException as e:
        return e.w_value()

class InterpException(Exception):
    def to_repr(self):
        return '#<SomeInterpException>'

class UndefinedVariable(InterpException):
    def __init__(self, w_sym):
        self._w_sym = w_sym

    def to_repr(self):
        return '#<UndefinedVariable %s>' % self._w_sym.name()

class HaltException(InterpException):
    def __init__(self, w_value):
        assert isinstance(w_value, W_Value)
        self._w_value = w_value

    def w_value(self):
        return self._w_value
