from rpython.jit.codewriter.policy import JitPolicy

from yasir.interp import interp
from yasir.simple import make_fibo

def target(config, argl):
    return main, None

def main(argl):
    try:
        n = int(argl[1])
    except (IndexError, TypeError) as e:
        n = 10
    w_res = interp(make_fibo(n))
    print('fibo(%d) = %s' % (n, w_res.to_pretty_string()))
    return 0

def jitpolicy(driver):
    return JitPolicy()

if __name__ == '__main__':
    import sys
    main(sys.argv)
