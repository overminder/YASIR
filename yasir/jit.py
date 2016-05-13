from rpython.rlib import jit

from . import pretty

def get_printable_location(expr, came_from):
    return pretty.nil().append_kw('expr', expr).append_kw('came_from', came_from).to_pretty_string()

jitdriver = jit.JitDriver(
    greens=['expr', 'came_from'],
    reds=['env', 'cont'],
    get_printable_location=get_printable_location,
    #should_unroll_one_iteration=lambda *args: True,
    #virtualizables=['env'],
    #is_recursive=True,
)
