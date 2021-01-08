package com.tencent.mtt.hippy.runtime.builtins.objects;

public class JSStringObject extends JSPrimitiveWrapper<CharSequence> {
  public JSStringObject(CharSequence value) {
    super(value);
  }
}
