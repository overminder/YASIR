from yasir import cereal, oop, ast

def test_open_file():
    bs = cereal.open_file_or_stdin(__file__).readall()
    assert bs.startswith('from yasir import cereal')

def test_deserialize_oop():
    cases = [
        ('\0', oop.w_nil),
        ('\1\0', oop.w_false),
        ('\1\1', oop.w_true),
        ('\2', oop.w_undef),
        ('\3\xff\xff\xff\xff', oop.W_Fixnum(-1)),
        ('\3\0\0\0\0', oop.W_Fixnum(0)),
        ('\3\x7f\xff\xff\xff', oop.W_Fixnum((2 ** 31) - 1)),
        ('\3\x80\x00\x00\x00', oop.W_Fixnum(-2 ** 31)),
        ('\4\x00\x03abc', oop.intern_symbol('abc')),
    ]
    for bs, expected in cases:
        assert cereal.deserialize_oop(cereal.BS(bs)).equals(expected)

def test_deserialize_ast():
    bs = '\0\x00\x03abc\x00\x01\x00\x00\x04\0'
    lam = cereal.deserialize_ast(cereal.BS(bs))
    assert isinstance(lam, ast.LambdaInfo)
    assert lam._name == 'abc'
    assert lam._arity == 1
    assert lam._frame_size == 1
    assert isinstance(lam._body, ast.Const)
    assert lam._body._w_value == oop.w_nil
