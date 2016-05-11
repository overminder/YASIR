from rpython.rlib import jit

from . import pretty

def get_printable_location(expr):
    return expr.to_pretty_string()

jitdriver = jit.JitDriver(greens=['expr'],
                          reds=['env', 'cont'],
                          should_unroll_one_iteration=lambda *args: True,
                          get_printable_location=get_printable_location)
                          # is_recursive=True)
