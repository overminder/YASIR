from yasir.oop import W_Fixnum, intern_symbol


def test_fixnum():
    a = W_Fixnum(1)
    b = W_Fixnum(2)
    assert a.ival() + b.ival() == 3


def test_intern_symbol():
    a = intern_symbol('a')
    a2 = intern_symbol('a')
    assert a is a2
    assert a.name() == a2.name()
