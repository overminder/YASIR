#!/usr/bin/env pypy

from rpython.jit.codewriter.policy import JitPolicy

from yasir.interp import interp
from yasir.simple import make_fibo, make_loop_sum

def target(config, argl):
    return main, None

def main(argl):
    try:
        n = int(argl[1])
        kind = argl[2]
    except (IndexError, TypeError) as e:
        n = 10
        kind = 'fibo'

    if kind == 'fibo':
        mk = make_fibo
    else:
        mk = make_loop_sum

    w_res = interp(mk(n))
    print('%s(%d) = %s' % (kind, n, w_res.to_pretty_string()))
    return 0

def jitpolicy(driver):
    return JitPolicy()

if __name__ == '__main__':
    import sys
    main(sys.argv)
