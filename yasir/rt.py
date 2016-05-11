# Contains nothing.
class BaseEnv(object):
    def __repr__(self):
        return self.to_repr()

    def to_repr(self):
        return '#<Env {}>'

    def lookup(self, w_sym, w_otherwise):
        return w_otherwise

    def extend(self, w_sym, w_value):
        return Env(w_sym, w_value, self)

nil_env = BaseEnv()

class Env(BaseEnv):
    def __init__(self, w_sym, w_value, prev=None):
        self._w_sym = w_sym
        self._w_value = w_value
        self._prev = prev

    def to_repr(self):
        thiz = self
        kvs = []
        while thiz is not nil_env:
            kvs.append('%s: %s' % (thiz._w_sym.name(), thiz._w_value.to_repr()))
            thiz = thiz._prev
        return '#<Env {%s}>' % ', '.join(kvs)

    def lookup(self, w_sym, w_otherwise):
        thiz = self
        while thiz is not nil_env:
            if thiz._w_sym is w_sym:
                return thiz._w_value
            else:
                thiz = thiz._prev
        return w_otherwise
