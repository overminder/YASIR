from rpython.rlib import jit

from . import pretty

# Contains nothing.
class BaseEnv(pretty.PrettyBase):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#env')

    #@jit.elidable
    def lookup(self, w_sym):
        from interp import UndefinedVariable
        raise UndefinedVariable(w_sym)

    def extend(self, w_sym, w_value):
        return Env(w_sym, w_value, self)

nil_env = BaseEnv()

class Env(BaseEnv):
    _immutable_ = True

    def __init__(self, w_sym, w_value, prev):
        self._w_sym = w_sym
        self._w_value = w_value
        self._prev = prev

    def to_pretty(self):
        thiz = self
        kvs = []
        while isinstance(thiz, Env):
            kvs.append(pretty.atom(':' + thiz._w_sym.name()))
            kvs.append(thiz._w_value)
            thiz = thiz._prev
        return pretty.atom('#env').extend(kvs)

    #@jit.elidable
    def lookup(self, w_sym):
        if self._w_sym is w_sym:
            return self._w_value
        else:
            return self._prev.lookup(w_sym)
