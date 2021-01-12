package com.tencent.mtt.hippy.runtime.builtins;

import java.nio.ByteBuffer;

public class JSSharedArrayBuffer extends JSArrayBuffer {
  public JSSharedArrayBuffer(ByteBuffer buffer) {
    super(buffer);
    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("ByteBuffer must be direct");
    }
  }
}
