from rpython.rlib import jit

from . import oop, pretty

class Cont(pretty.PrettyBase):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#<Cont>')

    # Not necessarily safe to call this directly.
    def plug_reduce(self, w_value, env):
        assert isinstance(w_value, oop.W_Value)
        raise NotImplementedError('Cont.plug_reduce: abstract method')


class Halt(Cont):
    _immutable_ = True

    def to_pretty(self):
        return pretty.atom('#halt')

    def plug_reduce(self, w_value, env):
        from .interp import HaltException
        raise HaltException(w_value)

# From pycket. This helps to avoid stack overflow for ReturnCont.
def label0(func, enter):
    from .ast import Expr
    func = jit.unroll_safe(func)

    class Label(Expr):
        _immutable_ = True
        should_enter = enter

        def evaluate(self, env, cont):
            assert isinstance(cont, ValueCont)
            w_value = cont._w_value
            prev = cont._cont
            return func(prev, w_value, env)

        def to_pretty(self):
            return pretty.atom('#label').append_kw('name', func.func_name) \
                                        .append_kw('module', func.__module__) \
                                        .append_kw('line', func.__code__.co_firstlineno)

    class ValueCont(Cont):
        _immutable_ = True

        def __init__(self, w_value, cont):
            self._w_value = w_value
            self._cont = cont

    label_instance = Label()

    def wraps(cont, w_value, env):
        return label_instance, env, ValueCont(w_value, cont)

    return wraps

def label(func): return label0(func, enter=False)
def loop_label(func): return label0(func, enter=True)
