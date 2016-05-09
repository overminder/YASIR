def target(config, argl):
    return main, None

def main(argl):
    print('hello, world')
    print('argl = %s' % argl)
    return 0

if __name__ == '__main__':
    import sys
    main(sys.argv)
