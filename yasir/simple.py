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
