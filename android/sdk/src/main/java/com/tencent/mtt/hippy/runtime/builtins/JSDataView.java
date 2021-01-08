package com.tencent.mtt.hippy.runtime.builtins;

import org.json.JSONException;
import org.json.JSONObject;

public class JSDataView<T extends JSArrayBuffer> extends JSObject {
  public enum DataViewKind {
    INT8_ARRAY, // kInt8Array
    UINT8_ARRAY, // kUint8Array
    UINT8_CLAMPED_ARRAY, // kUint8ClampedArray
    INT16_ARRAY, // kInt16Array
    UINT16_ARRAY, // kUint16Array
    INT32_ARRAY, // kInt32Array
    UINT32_ARRAY, // kUint32Array
    FLOAT32_ARRAY, // kFloat32Array
    FLOAT64_ARRAY, // kFloat64Array
    DATA_VIEW; // kDataView
  }

  private T bufferObject;
  private DataViewKind kind;
  private final String BYTE_OFFSET = "byteOffset";
  private final String BYTE_LENGTH = "byteLength";

  public JSDataView(T bufferObject, DataViewKind kind, int byteOffset, int byteLength) {
    this.bufferObject = bufferObject;
    this.kind = kind;
    set(BYTE_OFFSET, byteOffset);
    set(BYTE_LENGTH, byteLength);
  }

  public DataViewKind getKind() {
    return kind;
  }

  public T getBufferObject() {
    return bufferObject;
  }

  public int getByteOffset() {
    Object value = get(BYTE_OFFSET);
    assert(value != null);
    return (int) value;
  }

  public int getByteLength() {
    Object value = get(BYTE_LENGTH);
    assert(value != null);
    return (int) value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public JSDataView<T> clone() throws CloneNotSupportedException {
    JSDataView<T> dest = (JSDataView<T>) super.clone();;
    dest.kind = kind;
    dest.bufferObject = (T) bufferObject.clone();
    return dest;
  }

  @Override
  public Object dump() throws JSONException {
    JSONObject json = (JSONObject) super.dump();
    json.put("kind", kind);
    json.put("buffer", JSValue.dump(bufferObject));
    return json;
  }
}
