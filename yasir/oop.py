# A heap-allocated value.
class W_Value(object):
    def to_repr(self):
        return '#<W_Value>'

    def __repr__(self):
        return self.to_repr()


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
    _immutable_fields_ = '_ival'

    def __init__(self, ival):
        assert isinstance(ival, int)
        self._ival = ival

    def ival(self):
        return self._ival

    def to_repr(self):
        return '#<W_Fixnum %d>' % self.ival()


def make_symbol_interner(baseclass):
    class W_Symbol(baseclass):
        _immutable_fields_ = ['_name']

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
