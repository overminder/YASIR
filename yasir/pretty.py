from rpython.rlib.objectmodel import specialize
from rpython.rlib import jit

class PrettyBase(object):
    @jit.dont_look_inside
    def to_pretty(self):
        raise NotImplementedError('PrettyBase.to_pretty')

    def __repr__(self):
        # NOT_RPYTHON
        return self.to_pretty_string()

    @jit.dont_look_inside
    def to_pretty_string(self):
        return self.to_pretty().to_string()

class PrettyValue(PrettyBase):
    def to_string(self):
        raise NotImplementedError('PrettyValue.to_string: abstract')

    def to_pretty(self):
        return self

    def as_list(self):
        raise NotImplementedError('PrettyValue.as_list: abstract')

    @specialize.argtype(2)
    def append_kw(self, key, pv):
        pvs = self.as_list()
        pvs.append(atom(':' + key))
        pvs.append(atom(pv))
        return PrettyList(pvs)

    @specialize.argtype(1)
    def append(self, pv):
        pvs = self.as_list()
        pvs.append(atom(pv))
        return PrettyList(pvs)

    def extend(self, pbs):
        pvs = self.as_list()
        pvs.extend([pb.to_pretty() for pb in pbs])
        return PrettyList(pvs)

class PrettyAtom(PrettyValue):
    def __init__(self, sval):
        assert isinstance(sval, str)
        self._sval = sval

    def to_string(self):
        return self._sval

    def as_list(self):
        return [self]

class PrettyList(PrettyValue):
    def __init__(self, pvs):
        self._pvs = pvs

    def to_string(self):
        return '(%s)' % ' '.join([pv.to_string() for pv in self._pvs])

    def as_list(self):
        return list(self._pvs)

@specialize.argtype(0)
def atom(s):
    if s is None:
        return PrettyAtom('None')
    elif isinstance(s, bool):
        s = str(s)
    elif isinstance(s, int):
        s = str(s)
    elif isinstance(s, str):
        pass
    elif isinstance(s, PrettyValue):
        return s
    elif isinstance(s, PrettyBase):
        return s.to_pretty()
    else:
        raise TypeError('pretty.atom: unknown type %s' % s)
    return PrettyAtom(s)


def nil():
    return PrettyList([])

def many(xs):
    return PrettyList([x.to_pretty() for x in xs])
