package com.tencent.mtt.hippy.exception;

public class OutOfJavaArrayMaxSizeException extends OutOfJavaIntegerMaxValueException {
  public OutOfJavaArrayMaxSizeException(int excepted) {
    super(excepted);
  }
}
