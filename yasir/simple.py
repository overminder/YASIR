from rpython.rlib.objectmodel import specialize

from yasir import ast, oop


@specialize.argtype(0)
def unknown_lit(lit):
    return TypeError('Unknown lit %r for type %r' % (lit, type(lit)))


@specialize.argtype(0)
def wrap_lit(lit):
    if lit is None:
        w_value = oop.w_nil
    elif isinstance(lit, bool):
        w_value = oop.W_Bool.wrap(lit)
    elif isinstance(lit, int):
        w_value = oop.W_Fixnum(lit)
    else:
        raise unknown_lit(lit)
    return w_value


@specialize.argtype(0)
def lit_expr(lit):
    return ast.Const(wrap_lit(lit))


def make_fibo(arg):
    fibo_sym = oop.intern_symbol('fibo')
    n = oop.intern_symbol('n')

    fibo_body = ast.If(
        ast.LessThan(ast.ReadVar(0), lit_expr(2)),
        ast.ReadVar(0),
        ast.Add(ast.Apply(ast.ReadBox(ast.ReadVar(1, 1)),
                          [ast.Sub(ast.ReadVar(0), lit_expr(1))]),
                ast.Apply(ast.ReadBox(ast.ReadVar(1, 1)),
                          [ast.Sub(ast.ReadVar(0), lit_expr(2))])))

    fibo_def = ast.LambdaInfo('fibo_main', 1, 1, ast.Seq([
        ast.WriteBox(ast.ReadVar(1), ast.LambdaInfo('fibo',
                                                    1, 0, fibo_body)),
        ast.Apply(ast.ReadBox(ast.ReadVar(1)), [ast.ReadVar(0)]),
    ]))

    fibo_main = ast.Apply(fibo_def, [lit_expr(arg)])

    return fibo_main


def make_loop_sum(arg):
    loop_sym = oop.intern_symbol('loop_sum')
    n = oop.intern_symbol('n')
    s = oop.intern_symbol('s')

    loop = ast.Seq([
        ast.DefineVar(loop_sym, ast.MkBox(lit_expr(None))), ast.WriteBox(
            ast.ReadVar(loop_sym), ast.LambdaInfo('loop-sum',
                [n, s], ast.If(
                    ast.LessThan(
                        ast.ReadVar(n), lit_expr(1)),
                    ast.ReadVar(s),
                    ast.Apply(
                        ast.ReadBox(ast.ReadVar(loop_sym)), [ast.Sub(
                            ast.ReadVar(n), lit_expr(1)), ast.Add(
                                ast.ReadVar(n), ast.ReadVar(s))]), ))),
        ast.Apply(
            ast.ReadBox(ast.ReadVar(loop_sym)), [lit_expr(arg), lit_expr(0)])
    ])

    return loop
