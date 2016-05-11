from yasir import pretty

def test_atom():
    assert pretty.atom('a').to_string() == 'a'

def test_append():
    assert pretty.atom('a').append('b').append('c').to_string() == '(a b c)'

def test_nested_append():
    assert pretty.atom('a').append('b').append(pretty.nil().append('c')).to_string() == '(a b (c))'

def test_many():
    assert pretty.many([pretty.nil(), pretty.nil()]).to_string() == '(() ())'

def test_extend():
    assert pretty.nil().extend([pretty.atom('a'), pretty.atom('b')]).to_string() == '(a b)'
