from rpython.rlib import jit

from . import pretty

# A heap-allocated value.
class W_Value(pretty.PrettyBase):
    def to_pretty(self):
        return pretty.atom('#<W_Value>')

    def to_bool(self):
        return True

# The empty list.
class W_Nil(W_Value):
    def to_pretty(self):
        return pretty.nil()


w_nil = W_Nil()


# An undefined variable's value
class W_Undefined(W_Value):
    def to_pretty(self):
        return pretty.atom('#<undef>')


w_undef = W_Undefined()


class W_Fixnum(W_Value):
    _immutable_ = True

    def __init__(self, ival):
        assert isinstance(ival, int)
        self._ival = ival

    def ival(self):
        return self._ival

    def to_pretty(self):
        return pretty.atom(self._ival)


class W_Bool(W_Value):
    def to_pretty(self):
        if self is w_false:
            s = '#f'
        else:
            s = '#t'
        return pretty.atom(s)

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

        def to_pretty(self):
            return pretty.atom(self._name)

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

    def to_pretty(self):
        return pretty.atom('#box').append(self._w_value)

    def set_w(self, w_value):
        self._w_value = w_value

    def w_value(self):
        # XXX Not very true.
        return jit.promote(self._w_value)

class W_Lambda(W_Value):
    _immutable_ = True

    def __init__(self, info, env):
        self._info = info
        self._env = env

    def to_pretty(self):
        return pretty.atom('#lambda').append(self._info.name())

    def call(self, w_argvalues, cont):
        body, env = self._info.build_expr_and_env(w_argvalues, self._env)
        return body, env, cont
