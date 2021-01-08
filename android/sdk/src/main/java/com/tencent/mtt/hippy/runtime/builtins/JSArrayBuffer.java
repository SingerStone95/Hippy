package com.tencent.mtt.hippy.runtime.builtins;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.ByteBuffer;

public class JSArrayBuffer extends JSValue {
  static final short MAX_DUMP_LENGTH = 1024;

  private ByteBuffer buffer;

  public static JSArrayBuffer allocateDirect(int capacity) {
    return new JSArrayBuffer(ByteBuffer.allocateDirect(capacity));
  }
  public JSArrayBuffer(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

  @Override
  public JSArrayBuffer clone() throws CloneNotSupportedException {
    JSArrayBuffer clonedObject = (JSArrayBuffer) super.clone();
    clonedObject.buffer = buffer.duplicate();
    return clonedObject;
  }

  @Override
  public Object dump() throws JSONException {
    JSONArray json = new JSONArray();
    ByteBuffer dupBuffer = buffer.duplicate();
    for (short i = 0; i < dupBuffer.capacity() && i < MAX_DUMP_LENGTH; i++) {
      json.put(dupBuffer.get());
    }
    return json;
  }
}
