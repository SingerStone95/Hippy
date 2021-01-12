package com.tencent.mtt.hippy.serialization.memory.buffer;

import java.nio.ByteBuffer;

public interface Allocator<T extends ByteBuffer> {
  public T allocate(int capacity);
  public T expand(T buffer, int capacity);
  public T release(T buffer);
}
