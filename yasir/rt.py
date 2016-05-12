from rpython.rlib import jit

from . import oop, pretty

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
        w_res = env.get_any_here(ix)
        assert w_res is not None
        return w_res

    def get_fixnum(self, ix, depth=0):
        env = self.at_depth(depth)
        return env.get_fixnum_here(ix)

    def get_any_here(self, ix):
        assert ix >= 0
        assert ix < len(self._w_slots)
        return self._w_slots[ix]

    def get_fixnum_here(self, ix):
        w_res = self.get_any_here(ix)
        if isinstance(w_res, oop.W_Fixnum):
            return w_res.ival()
        else:
            raise EnvTypeMismatch()

    def get_bool_here(self, ix):
        w_res = self.get_any_here(ix)
        if isinstance(w_res, oop.W_Bool):
            return w_res.to_bool()
        else:
            raise EnvTypeMismatch()

    @staticmethod
    def extend(env, w_values):
        return Env(w_values, env)

class EnvTypeMismatch(Exception):
    pass

# Current impl
Env = ImmutableEnv

nil_env = None
