from rpython.rlib.objectmodel import specialize

from . import ast, oop

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


def make_make_fibo():
    fibo_sym = oop.intern_symbol('fibo')
    n = oop.intern_symbol('n')

    # It's crucial that any sharing between the AST nodes should be prohibited as
    # that will confuse the JIT's loop finder.
    mk_read_fibo = lambda: ast.ReadVar(1, 1)
    mk_read_n = lambda: ast.ReadVar(0)

    def make_fibo(arg):
        fibo_body = ast.If(
            ast.LessThan(mk_read_n(), lit_expr(2)),
            mk_read_n(),
            ast.Add(ast.Apply(ast.ReadBox(mk_read_fibo()),
                            [ast.Sub(mk_read_n(), lit_expr(1))]),
                    ast.Apply(ast.ReadBox(mk_read_fibo()),
                            [ast.Sub(mk_read_n(), lit_expr(2))])))

        fibo_def = ast.LambdaInfo('fibo_main', 1, 1, ast.Seq([
            ast.WriteBox(ast.ReadVar(1), ast.LambdaInfo('fibo',
                                                        1, 0, fibo_body)),
            ast.Apply(ast.ReadBox(ast.ReadVar(1)), [ast.ReadVar(0)]),
        ]))

        fibo_main = ast.Apply(fibo_def, [lit_expr(arg)])
        return fibo_main

    return make_fibo

make_fibo = make_make_fibo()
del make_make_fibo


def make_make_loop_sum():
    mk_read_loop = lambda: ast.ReadVar(1, 1)
    mk_read_n = lambda: ast.ReadVar(0)
    mk_read_s = lambda: ast.ReadVar(1)

    def make_loop_sum(arg):
        loop_body = ast.If(ast.LessThan(mk_read_n(), lit_expr(1)),
                           mk_read_s(),
                           ast.Apply(ast.ReadBox(mk_read_loop()),
                                     [ast.Sub(mk_read_n(), lit_expr(1)),
                                      ast.Add(mk_read_n(), mk_read_s())], is_tail=True))
        loop_trampo = ast.LambdaInfo('loop-sum-main', 1, 1, ast.Seq([
            ast.WriteBox(ast.ReadVar(1), ast.LambdaInfo('loop-sum', 2, 0, loop_body)),
            ast.Apply(ast.ReadBox(ast.ReadVar(1)), [ast.ReadVar(0), lit_expr(0)]),
        ]))

        return ast.Apply(loop_trampo, [lit_expr(arg)])

    return make_loop_sum

make_loop_sum = make_make_loop_sum()
del make_make_loop_sum
