package com.tencent.mtt.hippy;

public final class NativeAccess {
  private NativeAccess() {

  }

  // region HostObject
  public static native Object readHostObject(long delegate);
  public static native void writeHostObject(long delegate, Object object);
  // endregion

  // region SharedArrayBuffer
  public static native Object getSharedArrayBufferFromId(long delegate, int id);
  public static native int getSharedArrayBufferId(long delegate, Object sharedArrayBuffer);
  // endregion
}
