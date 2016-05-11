from rpython.rlib import jit

jitdriver = jit.JitDriver(greens=['expr'],
                          reds=['env', 'cont'])
                          # should_unroll_one_iteration=lambda *args: True,
                          # is_recursive=True)
