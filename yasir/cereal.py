import inspect

from rpython.rlib.streamio import open_file_as_stream, fdopen_as_stream
from rpython.rlib.unroll import unrolling_iterable
from rpython.rlib.objectmodel import specialize

from . import oop, ast


def open_file_or_stdin(path):
    if path == '-':
        return fdopen_as_stream(0, 'r')
    else:
        return open_file_as_stream(path)


def deserialize_ast_from_file(path):
    bs = open_file_or_stdin(path).readall()
    return deserialize_ast(BS(bs))


class BS(object):
    def __init__(self, bs, ix=0):
        assert ix >= 0
        self._bs = bs
        self._ix = ix

    def has_next(self):
        return self._ix < len(self._bs)

    def next_string(self, count):
        return ''.join(self.next_many(count, BS.next_char))

    def next_char(self):
        return chr(self.next_u8())

    def next_u8(self):
        assert self.has_next()
        b = self._bs[self._ix]
        self._ix += 1
        return ord(b)

    def next_u16(self):
        # Big-endian.
        hi = self.next_u8()
        lo = self.next_u8()
        return (hi << 8) | lo

    def next_u32(self):
        hi = self.next_u16()
        lo = self.next_u16()
        return (hi << 16) | lo

    @specialize.argtype(2)
    def next_many(self, count, dfunc):
        xs = []
        for _ in range(count):
            xs.append(dfunc(self))
        return xs


def deserialize_ast(r):
    tag = r.next_u8()
    for i, d in unrolling_iterable(enumerate(ast_ds)):
        if tag == i:
            return d(r)
    assert False, 'Unknown tag: %d' % tag


d_ast = deserialize_ast


def d_applicative(ctor, *ds):
    def wraps(r):
        args = ()
        for d in unrolling_iterable(ds):
            args += (d(r), )
        return ctor(*args)

    return wraps


def d_many_ast(r):
    n = r.next_u16()
    return r.next_many(n, d_ast)


def d_prim_ast(ctor):
    arity = ctor.ctor_arity
    return d_applicative(ctor, *([d_ast] * arity))


def deserialize_oop(r):
    tag = r.next_u8()
    if tag == O_NIL:
        return oop.w_nil
    elif tag == O_BOOL:
        bval = r.next_u8()
        return oop.W_Bool.wrap(bval)
    elif tag == O_UNDEF:
        return oop.w_undef
    elif tag == O_FIXNUM:
        uval = r.next_u32()
        if uval > (2**31) - 1:
            uval -= 2**32
        return oop.W_Fixnum(uval)
    elif tag == O_SYMBOL:
        return oop.intern_symbol(d_string(r))
    else:
        assert False, 'Unknown tag: %d' % tag


d_oop = deserialize_oop

O_NIL = 0
O_BOOL = 1
O_UNDEF = 2
O_FIXNUM = 3
O_SYMBOL = 4


def d_string(r):
    n = r.next_u16()
    return r.next_string(n)


ast_ds = [
    d_applicative(ast.LambdaInfo, d_string, BS.next_u16, BS.next_u16, d_ast),
    d_applicative(ast.Apply, d_ast, d_many_ast, BS.next_u8),
    d_applicative(ast.ReadVar, BS.next_u16, BS.next_u16),
    d_applicative(ast.Seq, d_many_ast),
    d_applicative(ast.Const, d_oop),
    d_prim_ast(ast.Add),
    d_prim_ast(ast.Sub),
    d_prim_ast(ast.LessThan),
    d_prim_ast(ast.MkBox),
    d_prim_ast(ast.ReadBox),
    d_prim_ast(ast.WriteBox),
]
