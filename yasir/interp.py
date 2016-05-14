from rpython.rlib import jit

from . import oop, ast
from .cont import Halt
from .rt import Env, nil_env, HaltException
from .jit import jitdriver

def interp(expr, env=nil_env):
    assert isinstance(expr, ast.Expr)

    cont = Halt()
    try:
        while True:
            jitdriver.jit_merge_point(expr=expr, env=env, cont=cont)
            # print('interp: %s %s %s' % (expr, env, cont))
            expr, env, cont = expr.evaluate(env, cont)
            if expr.should_enter:
                jitdriver.can_enter_jit(expr=expr, env=env, cont=cont)
    except HaltException as e:
        return e.w_value()
