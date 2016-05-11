from rpython.rlib import jit

# A heap-allocated value.
class W_Value(object):
    def to_repr(self):
        return '#<W_Value>'

    def __repr__(self):
        return self.to_repr()

    def to_bool(self):
        return True

# The empty list.
class W_Nil(W_Value):
    def to_repr(self):
        return '#<W_Nil>'


w_nil = W_Nil()


# An undefined variable's value
class W_Undefined(W_Value):
    def to_repr(self):
        return '#<W_Undefined>'


w_undef = W_Undefined()


class W_Fixnum(W_Value):
    _immutable_ = True

    def __init__(self, ival):
        assert isinstance(ival, int)
        self._ival = ival

    def ival(self):
        return self._ival

    def to_repr(self):
        return '#<W_Fixnum %d>' % self.ival()


class W_Bool(W_Value):
    def to_repr(self):
        if self is w_false:
            return '#f'
        else:
            return '#t'

    def to_bool(self):
        return self is not w_false

    @staticmethod
    def wrap(bval):
        if bval:
            return w_true
        else:
            return w_false

w_true = W_Bool()
w_false = W_Bool()

def make_symbol_interner(baseclass):
    class W_Symbol(baseclass):
        _immutable_ = True

        def __init__(self, name):
            assert isinstance(name, str)
            self._name = name

        def name(self):
            return self._name

        def to_repr(self):
            return '#<W_Symbol %s>' % self.name()

    symbols = {}

    def intern_symbol(s):
        res = symbols.get(s, None)
        if res is None:
            res = W_Symbol(s)
            symbols[s] = res
        return res

    return intern_symbol, W_Symbol


intern_symbol, W_Symbol = make_symbol_interner(W_Value)
del make_symbol_interner

class W_Box(W_Value):
    def __init__(self, w_value):
        self.set_w(w_value)

    def to_repr(self):
        return '#<W_Box %s>' % self._w_value.to_repr()

    def set_w(self, w_value):
        self._w_value = w_value

    def w_value(self):
        return self._w_value

class W_Lambda(W_Value):
    _immutable_ = True

    def __init__(self, w_argnames, body, env):
        self._w_argnames = w_argnames
        self._body = body
        self._env = env

    def to_repr(self):
        return '#<W_Lambda %s>' % (self._w_argnames,)

    @jit.unroll_safe
    def call(self, w_argvalues, cont):
        from .rt import Env

        assert len(w_argvalues) == len(self._w_argnames)

        # Populate env.
        env = self._env
        for i in range(len(w_argvalues)):
            env = env.extend(self._w_argnames[i], w_argvalues[i])

        return self._body, env, cont
