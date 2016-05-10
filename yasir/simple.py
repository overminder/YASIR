from yasir import ast, oop

def unknown_lit(lit):
    return TypeError('Unknown lit %r for type %r' % (lit, type(lit)))

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

def lit_expr(lit):
    return ast.Const(wrap_lit(lit))

def make_fibo():
    fibo_sym = oop.intern_symbol('fibo')
    n = oop.intern_symbol('n')

    fibo = ast.Seq([
        ast.DefineVar(fibo_sym, ast.MkBox(lit_expr(None))),
        ast.WriteBox(ast.ReadVar(fibo_sym),
                     ast.Lambda([n],
                                ast.If(ast.LessThan(ast.ReadVar(n), lit_expr(2)),
                                       ast.ReadVar(n),
                                       ast.Add(ast.Apply(ast.ReadBox(ast.ReadVar(fibo_sym)),
                                                         [ast.Sub(ast.ReadVar(n), lit_expr(1))]),
                                               ast.Apply(ast.ReadBox(ast.ReadVar(fibo_sym)),
                                                         [ast.Sub(ast.ReadVar(n), lit_expr(2))]),
                                               )))),
        ast.Apply(ast.ReadBox(ast.ReadVar(fibo_sym)), [lit_expr(3)])
    ])

    return fibo

fibo = make_fibo()
