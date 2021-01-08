package com.tencent.mtt.hippy.runtime.builtins.objects;

import java.math.BigInteger;

public class JSBigintObject extends JSPrimitiveWrapper<BigInteger> {
  public JSBigintObject(BigInteger value) {
    super(value);
  }
}
