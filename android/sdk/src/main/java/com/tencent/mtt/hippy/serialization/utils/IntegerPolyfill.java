package com.tencent.mtt.hippy.serialization.utils;

public final class IntegerPolyfill {
  private IntegerPolyfill() {

  }

  public static long toUnsignedLong(int x) {
    return ((long) x) & 0xffffffffL;
  }
}
