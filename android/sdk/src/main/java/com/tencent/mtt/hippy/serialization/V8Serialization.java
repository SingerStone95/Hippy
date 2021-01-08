package com.tencent.mtt.hippy.serialization;

import java.nio.ByteOrder;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public abstract class V8Serialization {
  static protected final byte VERSION = (byte) 0xFF; // SerializationTag::kVersion
  static protected final byte LATEST_VERSION = (byte) 13; // kLatestVersion
  static protected final String NATIVE_UTF16_ENCODING = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? "UTF-16BE" : "UTF-16LE";

  protected final Object Null;
  protected final Object Undefined;
  protected final Object Hole;

  V8Serialization() {
    Null = getNull();
    Undefined = getUndefined();
    Hole = getHole();
  }

  protected abstract Object getUndefined();
  protected abstract Object getNull();
  protected abstract Object getHole();
}
