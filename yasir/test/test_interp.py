import pytest

from yasir import ast, oop
from yasir.interp import interp
from yasir.simple import wrap_lit, lit_expr, unknown_lit, make_fibo, make_loop_sum


def assert_evaluates_to_lit(expr, lit):
    w_res = interp(expr)
    # XXX: Would like to rather use value equality.
    if lit is None:
        assert w_res is wrap_lit(lit)
    elif isinstance(lit, bool):
        assert w_res is wrap_lit(lit)
    elif isinstance(lit, int):
        assert isinstance(w_res, oop.W_Fixnum)
        assert w_res.ival() == lit
    else:
        raise unknown_lit(lit)


def test_const():
    assert_evaluates_to_lit(lit_expr(42), 42)


def test_add():
    assert_evaluates_to_lit(ast.Add(lit_expr(40), lit_expr(2)), 42)
    assert_evaluates_to_lit(
        ast.Add(
            lit_expr(40), ast.Add(
                lit_expr(1), lit_expr(1))), 42)


def test_define_simple():
    assert_evaluates_to_lit(
        ast.DefineVar(
            oop.intern_symbol('a'), lit_expr(42)), None)


def test_readvar_undef():
    from yasir.interp import UndefinedVariable
    with pytest.raises(UndefinedVariable):
        assert_evaluates_to_lit(ast.ReadVar(oop.intern_symbol('a')), None)


def test_define_readvar():
    a = oop.intern_symbol('a')
    assert_evaluates_to_lit(
        ast.Seq([ast.DefineVar(a, lit_expr(42)), ast.ReadVar(a)]), 42)


def test_const_lambda():
    x = oop.intern_symbol('x')
    assert_evaluates_to_lit(
        ast.Apply(
            ast.LambdaInfo('', [x], ast.ReadVar(x)), [lit_expr(42)]), 42)


def test_if():
    assert_evaluates_to_lit(
        ast.If(
            lit_expr(False), lit_expr(0), lit_expr(42)), 42)


def test_lessthan():
    assert_evaluates_to_lit(ast.LessThan(lit_expr(1), lit_expr(2)), True)
    assert_evaluates_to_lit(ast.LessThan(lit_expr(2), lit_expr(1)), False)


def test_box():
    x = oop.intern_symbol('x')
    assert_evaluates_to_lit(
        ast.Seq([
            ast.DefineVar(x, ast.MkBox(lit_expr(0))), ast.WriteBox(
                ast.ReadVar(x), lit_expr(42)), ast.ReadBox(ast.ReadVar(x))
        ]), 42)


def test_fibo():
    assert_evaluates_to_lit(make_fibo(10), 55)


def test_loop():
    assert_evaluates_to_lit(make_loop_sum(10), 55)
