from rpython.rlib.objectmodel import specialize

from . import ast, oop, config

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

    if config.USE_LINKED_ENV:
        read_fibo = ast.ReadVar(fibo_sym)
        read_n = ast.ReadVar(n)
    else:
        read_fibo = ast.ReadVar(1, 1)
        read_n = ast.ReadVar(0)

    fibo_body = ast.If(
        ast.LessThan(read_n, lit_expr(2)),
        read_n,
        ast.Add(ast.Apply(ast.ReadBox(read_fibo),
                          [ast.Sub(read_n, lit_expr(1))]),
                ast.Apply(ast.ReadBox(read_fibo),
                          [ast.Sub(read_n, lit_expr(2))])))

    if config.USE_LINKED_ENV:
        arg_sym = oop.intern_symbol('arg')
        fibo_def = ast.LambdaInfo('fibo_main', [arg_sym], ast.Seq([
            ast.DefineVar(fibo_sym, ast.MkBox(ast.Const(oop.w_nil))),
            ast.WriteBox(read_fibo, ast.LambdaInfo('fibo', [n], fibo_body)),
            ast.Apply(ast.ReadBox(read_fibo), [ast.ReadVar(arg_sym)]),
        ]))
    else:
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
