from yasir import ast, oop
from yasir.interp import interp


def unknown_lit(lit):
    return TypeError('Unknown lit %r for type %r' % (lit, type(lit)))


def const_expr(lit):
    if isinstance(lit, int):
        w_value = oop.W_Fixnum(lit)
    else:
        raise unknown_lit(lit)
    return ast.Const(w_value)


def assert_evaluates_to_lit(expr, lit):
    res = interp(expr)
    # XXX: Would like to rather use value equality.
    if isinstance(lit, int):
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
