# Contains nothing.
class BaseEnv(object):
    _immutable_ = True

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
    _immutable_ = True

    def __init__(self, w_sym, w_value, prev):
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
        if self._w_sym is w_sym:
            return self._w_value
        else:
            return self._prev.lookup(w_sym, w_otherwise)
