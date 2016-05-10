from yasir.oop import W_Fixnum, intern_symbol

def target(config, argl):
    return main, None

def main(argl):
    print('hello, world')
    print('argl = %s' % argl)
    a = intern_symbol('a')
    a2 = intern_symbol('a')
    print('a = %s' % a.to_repr())
    return 0

if __name__ == '__main__':
    import sys
    main(sys.argv)
