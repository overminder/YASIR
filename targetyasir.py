#!/usr/bin/env pypy

from rpython.jit.codewriter.policy import JitPolicy

from yasir.interp import interp
from yasir.cereal import deserialize_ast_from_file
from yasir.simple import make_fibo, make_loop_sum

def target(config, argl):
    return main, None

def main(argl):
    try:
        (kind, args) = argl[1:]
        if kind == 'fibo':
            n = int(args)
            expr = make_fibo(n)
            comment = 'fibo(%d)' % n
        elif kind == 'loop':
            n = int(args)
            expr = make_loop_sum(n)
            comment = 'loop-sum(%d)' % n
        elif kind == 'run-file':
            expr = deserialize_ast_from_file(args)
            comment = spec
    except (IndexError, ValueError) as e:
        print 'USAGE: %s KIND ARGS' % argl[0]
        print '  where KIND is one of (fibo | loop | run-file)'
        return 1

    w_res = interp(expr)
    print('%s => %s' % (comment, w_res.to_pretty_string()))
    return 0

def jitpolicy(driver):
    return JitPolicy()

if __name__ == '__main__':
    import sys
    main(sys.argv)
