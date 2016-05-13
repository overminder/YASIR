from rpython.rlib import jit

from . import pretty

class ImmutableEnv(object):
    _immutable_fields_ = ['_w_slots[*]', '_prev']

    def __init__(self, w_values, prev):
        self._w_slots = w_values
        self._prev = prev

    @jit.unroll_safe
    def at_depth(self, depth):
        #depth = jit.promote(depth)
        i = depth
        it = self
        while i > 0:
            assert it._prev is not None
            it = it._prev
            i = i - 1
        return it

    # Don't use elidable here.
    def get(self, ix, depth=0):
        env = self.at_depth(depth)
        w_res = env.get_here(ix)
        assert w_res is not None
        return w_res

    def get_here(self, ix):
        assert ix >= 0
        assert ix < len(self._w_slots)
        return self._w_slots[ix]

    @staticmethod
    def extend(env, w_values):
        return Env(w_values, env)


Env = ImmutableEnv
nil_env = Env([], None)


class InterpException(Exception):
    def to_repr(self):
        return '#<SomeInterpException>'

    def __repr__(self):
        return self.to_repr()

class UndefinedVariable(InterpException):
    def __init__(self, w_sym):
        self._w_sym = w_sym

    def to_repr(self):
        return '#<UndefinedVariable %s>' % self._w_sym.name()

class HaltException(InterpException):
    def __init__(self, w_value):
        from . import oop
        assert isinstance(w_value, oop.W_Value)
        self._w_value = w_value

    def w_value(self):
        return self._w_value

