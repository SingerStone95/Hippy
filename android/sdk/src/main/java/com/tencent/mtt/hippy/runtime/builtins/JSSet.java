package com.tencent.mtt.hippy.runtime.builtins;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;

public class JSSet extends JSValue {
  private final HashSet<Object> internalSet;

  public JSSet() {
    this.internalSet = new HashSet<>();
  }

  public HashSet<Object> getInternalSet() {
    return internalSet;
  }

  @Override
  public JSSet clone() throws CloneNotSupportedException {
    JSSet clonedObject = (JSSet) super.clone();
    HashSet<Object> destSet = clonedObject.getInternalSet();
    for (Object o : internalSet) {
      destSet.add(JSValue.clone(o));
    }
    return clonedObject;
  }

  @Override
  public Object dump() throws JSONException {
    JSONArray json = new JSONArray();
    for (Object o : internalSet) {
      json.put(JSValue.dump(o));
    }
    return json;
  }
}
