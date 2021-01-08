package com.tencent.mtt.hippy.runtime.builtins.objects;

public class JSBooleanObject extends JSPrimitiveWrapper<Boolean> {
  public static final JSBooleanObject True = new JSBooleanObject(true);
  public static final JSBooleanObject False = new JSBooleanObject(false);

  private JSBooleanObject(boolean value) {
    super(value);
  }

  public final boolean isTrue() {
    return getValue();
  }

  public final boolean isFalse() {
    return !getValue();
  }

  @Override
  @SuppressWarnings("all") // "CloneDoesntCallSuperClone" not recognized
  public final JSBooleanObject clone() {
    return this;
  }
}
