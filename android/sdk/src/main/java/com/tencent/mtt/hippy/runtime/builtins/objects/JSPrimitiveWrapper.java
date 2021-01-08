package com.tencent.mtt.hippy.runtime.builtins.objects;

import com.tencent.mtt.hippy.runtime.builtins.JSValue;

import org.json.JSONException;

public abstract class JSPrimitiveWrapper<T> extends JSValue {
  private final T value;

  public JSPrimitiveWrapper(T value) {
    this.value = value;
  }

  public final T getValue() {
    return value;
  }

  @Override
  public final Object dump() throws JSONException {
    return value;
  }
}
