from yasir.interp import interp
from yasir.simple import fibo

def target(config, argl):
    return main, None

def main(argl):
    w_res = interp(fibo)
    print('w_res = %s' % w_res.to_repr())
    return 0

if __name__ == '__main__':
    import sys
    main(sys.argv)
