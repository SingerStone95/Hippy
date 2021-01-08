package com.tencent.mtt.hippy.serialization;

public enum ArrayBufferViewTag {
  INT8_ARRAY('b'), // kInt8Array
  UINT8_ARRAY('B'), // kUint8Array
  UINT8_CLAMPED_ARRAY('C'), // kUint8ClampedArray
  INT16_ARRAY('w'), // kInt16Array
  UINT16_ARRAY('W'), // kUint16Array
  INT32_ARRAY('d'), // kInt32Array
  UINT32_ARRAY('D'), // kUint32Array
  FLOAT32_ARRAY('f'), // kFloat32Array
  FLOAT64_ARRAY('F'), // kFloat64Array
  DATA_VIEW('?'); // kDataView

  private final byte tag;

  ArrayBufferViewTag(char tag) {
    this.tag = (byte) tag;
  }

  public byte getTag() {
    return tag;
  }

  public static ArrayBufferViewTag fromTag(byte tag) {
    for (ArrayBufferViewTag t : values()) {
      if (t.tag == tag) {
        return t;
      }
    }
    return null;
  }
}
