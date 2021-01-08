package com.tencent.mtt.hippy.serialization;

public class UnsupportedTagException extends RuntimeException {
  public UnsupportedTagException(SerializationTag tag) {
    super("Deserialization of a value tagged " + tag);
  }
}
