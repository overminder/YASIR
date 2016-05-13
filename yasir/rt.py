from rpython.rlib import jit

from . import pretty, config

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
        from . import oop
        w_res = self.get_any_here(ix)
        if isinstance(w_res, oop.W_Fixnum):
            return w_res.ival()
        else:
            raise EnvTypeMismatch()

    def get_bool_here(self, ix):
        from . import oop
        w_res = self.get_any_here(ix)
        if isinstance(w_res, oop.W_Bool):
            return w_res.to_bool()
        else:
            raise EnvTypeMismatch()

    @staticmethod
    def extend(env, w_values):
        return Env(w_values, env)

class LinkedEnvBase(object):
    def lookup(self, w_sym):
        from . import oop
        return self._locate(w_sym)._get()

    def set_at(self, w_sym, w_value):
        env = self._locate(w_sym)
        assert isinstance(env, LinkedMutableEnv)
        env.set(w_value)

    def _locate(self, w_sym):
        raise UndefinedVariable(w_sym)

    def _get(self):
        raise TypeError('Should not reach here')

    @staticmethod
    def extend(env, w_sym, w_value, mutable=False):
        from . import oop
        if mutable:
            # XXX: Need to reconsider this - must ensure that the box
            # never escapes.
            return LinkedMutableEnv(w_sym, w_value, env)
        else:
            if isinstance(w_value, oop.W_Fixnum):
                return LinkedFixnumEnv(w_sym, w_value.ival(), env)
            else:
                return LinkedAnyEnv(w_sym, w_value, env)

class LinkedNamedEnv(LinkedEnvBase):
    _immutable_fields_ = ['_w_sym', '_prev']

    def __init__(self, w_sym, prev):
        self._w_sym = w_sym
        self._prev = prev

    def _locate(self, w_sym):
        if self._w_sym is w_sym:
            return self
        else:
            return self._prev._locate(w_sym)

class LinkedFixnumEnv(LinkedNamedEnv):
    _immutable_ = True

    def __init__(self, w_sym, ival, prev):
        LinkedNamedEnv.__init__(self, w_sym, prev)
        self._ival = ival

    def _get(self):
        from . import oop
        return oop.W_Fixnum(self._ival)

class LinkedAnyEnv(LinkedNamedEnv):
    _immutable_ = True

    def __init__(self, w_sym, w_value, prev):
        LinkedNamedEnv.__init__(self, w_sym, prev)
        self._w_value = w_value

    def _get(self):
        return self._w_value

class LinkedMutableEnv(LinkedNamedEnv):
    def __init__(self, w_sym, w_value, prev):
        LinkedNamedEnv.__init__(self, w_sym, prev)
        self._w_value = w_value

    def set(self, w_value):
        self._w_value = w_value

    def _get(self):
        return self._w_value

if config.USE_LINKED_ENV:
    Env = LinkedEnvBase
    nil_env = Env()
else:
    Env = ImmutableEnv
    nil_env = None


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

class EnvTypeMismatch(Exception):
    pass
