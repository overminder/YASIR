from .ast import Expr, Halt
from .oop import W_Value


def interp(expr, env=None):
    assert isinstance(expr, Expr)

    cont = Halt()
    try:
        while True:
            expr, env, cont = expr.evaluate(env, cont)
    except HaltException as e:
        return e.w_value()


class HaltException(Exception):
    def __init__(self, w_value):
        assert isinstance(w_value, W_Value)
        self._w_value = w_value

    def w_value(self):
        return self._w_value
