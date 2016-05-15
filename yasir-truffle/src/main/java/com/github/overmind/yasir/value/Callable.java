package com.github.overmind.yasir.value;

import com.oracle.truffle.api.CallTarget;

/**
  * Created by overmind on 5/15/16.
  */
public abstract class Callable {
  public abstract Object payload();
  public abstract CallTarget target();
}
