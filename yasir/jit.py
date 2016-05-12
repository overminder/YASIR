from rpython.rlib import jit

from . import pretty

def get_printable_location(expr):
    return expr.to_pretty_string()

jitdriver = jit.JitDriver(
    greens=['expr'],
    reds=['env', 'cont'],
    get_printable_location=get_printable_location,
    #should_unroll_one_iteration=lambda *args: True,
    #virtualizables=['env'],
    #is_recursive=True,
)
