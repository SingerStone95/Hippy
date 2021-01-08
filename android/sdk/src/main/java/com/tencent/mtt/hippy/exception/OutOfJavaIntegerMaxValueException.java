package com.tencent.mtt.hippy.exception;

public class OutOfJavaIntegerMaxValueException extends IndexOutOfBoundsException {
  private static final long UINT32_MAX_VALUE = 4294967296L;  // uint32(2^32)

  public OutOfJavaIntegerMaxValueException(int excepted) {
    super("Excepted:" + (UINT32_MAX_VALUE + excepted) + "(" + excepted + ")");
  }
}
