import pytest

from yasir import ast, oop
from yasir.interp import interp


def unknown_lit(lit):
    return TypeError('Unknown lit %r for type %r' % (lit, type(lit)))


def const_expr(lit):
    if lit is None:
        w_value = oop.w_nil
    elif isinstance(lit, int):
        w_value = oop.W_Fixnum(lit)
    else:
        raise unknown_lit(lit)
    return ast.Const(w_value)


def assert_evaluates_to_lit(expr, lit):
    res = interp(expr)
    # XXX: Would like to rather use value equality.
    if lit is None:
        assert res is oop.w_nil
    elif isinstance(lit, int):
        assert isinstance(res, oop.W_Fixnum)
        assert res.ival() == lit
    else:
        raise unknown_lit(lit)


def test_const():
    assert_evaluates_to_lit(const_expr(42), 42)


def test_add():
    assert_evaluates_to_lit(ast.Add(const_expr(40), const_expr(2)), 42)
    assert_evaluates_to_lit(
        ast.Add(
            const_expr(40), ast.Add(
                const_expr(1), const_expr(1))), 42)


def test_define_simple():
    assert_evaluates_to_lit(
        ast.DefineVar(
            oop.intern_symbol('a'), const_expr(42)), None)


def test_readvar_undef():
    from yasir.interp import UndefinedVariable
    with pytest.raises(UndefinedVariable):
        assert_evaluates_to_lit(ast.ReadVar(oop.intern_symbol('a')), None)


def test_define_readvar():
    a = oop.intern_symbol('a')
    assert_evaluates_to_lit(
        ast.Seq([ast.DefineVar(a, const_expr(42)), ast.ReadVar(a)]), 42)
