package com.tencent.mtt.hippy.runtime.builtins;

import org.json.JSONException;

interface JSONDump {
  public Object dump() throws JSONException;
}
