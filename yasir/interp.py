from rpython.rlib import jit

from . import oop, ast
from .rt import Env, nil_env
from .jit import jitdriver

def interp(expr, env=nil_env):
    assert isinstance(expr, ast.Expr)

    cont = ast.Halt()
    try:
        while True:
            jitdriver.jit_merge_point(expr=expr, env=env, cont=cont)
            # print('interp: %s %s %s' % (expr, env, cont))
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
        assert isinstance(w_value, oop.W_Value)
        self._w_value = w_value

    def w_value(self):
        return self._w_value
