package com.tencent.mtt.hippy.serialization.exception;

import com.tencent.mtt.hippy.serialization.exception.DataCloneOutOfValueException;

public class DataCloneOutOfRangeException extends DataCloneOutOfValueException {
  public DataCloneOutOfRangeException(int excepted) {
    super(excepted);
  }
}
