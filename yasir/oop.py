class W_Root(object):
    def __repr__(self):
        return '#<W_Root>'

class W_Fixnum(W_Root):
    _ival = 0
    def __init__(self, ival):
        self._ival = ival

    def ival(self):
        return self._ival

    def __repr__(self):
        return '#<W_Fixnum %d>' % self.ival()

def make_symbol_interner(baseclass):
    class W_Symbol(baseclass):
        _name = '#unnamed-symbol'
        def __init__(self, name):
            self._name = name

        def name(self):
            return self._name

        def __repr__(self):
            return '#<W_Symbol %s>' % self.name()

    symbols = {}

    def intern_symbol(s):
        res = symbols.get(s, None)
        if res is None:
            res = W_Symbol(s)
            symbols[s] = res
        return res

    return intern_symbol, W_Symbol

intern_symbol, W_Symbol = make_symbol_interner(W_Root)
