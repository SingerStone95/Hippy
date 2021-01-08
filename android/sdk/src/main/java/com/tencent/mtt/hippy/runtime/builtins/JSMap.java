package com.tencent.mtt.hippy.runtime.builtins;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSMap extends JSValue {
  private final HashMap<Object, Object> internalMap;
  public JSMap() {
    internalMap = new HashMap<>();
  }

  public HashMap<Object, Object> getInternalMap() {
    return internalMap;
  }

  @Override
  public JSMap clone() throws CloneNotSupportedException {
    JSMap clonedObject = (JSMap) super.clone();
    HashMap<Object, Object> destMap = clonedObject.getInternalMap();
    for (Map.Entry<Object, Object> entry : internalMap.entrySet()) {
      destMap.put(entry.getKey(), JSValue.clone(entry.getValue()));
    }
    return clonedObject;
  }

  @Override
  public Object dump() throws JSONException {
    JSONObject json = new JSONObject();
    for (Map.Entry<Object, Object> entry : internalMap.entrySet()) {
      json.put(entry.getKey().toString(), JSValue.dump(entry.getValue()));
    }
    return json;
  }
}
