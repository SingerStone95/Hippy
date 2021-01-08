package com.tencent.mtt.hippy.runtime.builtins.objects;

public class JSNumberObject extends JSPrimitiveWrapper<Number> {
  public JSNumberObject(Number value) {
    super(value);
  }
}
