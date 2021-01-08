package com.tencent.mtt.hippy.runtime.utils;

import android.util.Pair;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.runtime.builtins.JSObject;
import com.tencent.mtt.hippy.runtime.builtins.JSSet;
import com.tencent.mtt.hippy.runtime.builtins.JSValue;
import com.tencent.mtt.hippy.runtime.builtins.array.JSDenseArray;
import com.tencent.mtt.hippy.runtime.builtins.array.JSSparseArray;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSPrimitiveWrapper;

import java.util.Map;

public class ValueConverter {
  static public Object toHippyValue(Object value) {
    if (JSValue.is(value)) {
      JSValue jsValue = (JSValue) value;
      if (jsValue.isPrimitiveObject()) {
        return ((JSPrimitiveWrapper<?>) value).getValue();
      }
      if (jsValue.isSet()) {
        HippyArray array = new HippyArray();
        for (Object o: ((JSSet) value).getInternalSet()) {
          array.pushObject(toHippyValue(o));
        }
        return array;
      }
      if (jsValue.isArray()) {
        if (jsValue.isDenseArray()) {
          HippyArray array = new HippyArray();
          for (Object o : ((JSDenseArray) value).items()) {
            array.pushObject(toHippyValue(o));
          }
          return array;
        } else if (jsValue.isSparseArray()) {
          HippyMap object = new HippyMap();
          for (Pair<Integer, Object> o : ((JSSparseArray) value).items()) {
            object.pushObject(String.valueOf(o.first), toHippyValue((o.second)));
          }
          return object;
        }
      }
      if (jsValue.isObject()) {
        HippyMap object = new HippyMap();
        for (Pair<String, Object> prop : ((JSObject) jsValue).entries()) {
          object.pushObject(prop.first, toHippyValue(prop.second));
        }
        return object;
      }
    }
    return value;
  }

  static public Object toJSValue(Object value) {
    if (value instanceof HippyArray) {
      HippyArray array = ((HippyArray) value);
      JSDenseArray jsArray = new JSDenseArray(array.size());
      for (int i = 0; i < array.size(); i++) {
        jsArray.push(toJSValue(array.get(i)));
      }
      return jsArray;
    } else if (value instanceof HippyMap) {
      HippyMap object = ((HippyMap) value);
      JSObject jsObject = new JSObject();
      for (Map.Entry<String, Object> entry : object.entrySet()) {
        jsObject.set(entry.getKey(), toJSValue(entry.getValue()));
      }
      return jsObject;
    }
    return value;
  }
}
