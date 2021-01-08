package com.tencent.mtt.hippy.serialization;

public class IntegerPolyfill {
  public static long toUnsignedLong(int x) {
    return ((long) x) & 0xffffffffL;
  }
}
