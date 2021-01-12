package com.tencent.mtt.hippy.serialization.memory.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SimpleAllocator implements Allocator<ByteBuffer> {
  protected static ByteBuffer allocateDirect(int capacity) {
    return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
  }

  @Override
  public ByteBuffer allocate(int capacity) {
     return allocateDirect(capacity);
  }

  @Override
  public ByteBuffer expand(ByteBuffer buffer, int capacityNeeded) {
    if (capacityNeeded > buffer.capacity()) {
      int newCapacity = Math.max(capacityNeeded, 2 * buffer.capacity());
      ByteBuffer newBuffer = allocateDirect(newCapacity);
      buffer.flip();
      newBuffer.put(buffer);
      buffer = newBuffer;
    }
    return buffer;
  }

  @Override
  public ByteBuffer release(ByteBuffer buffer) {
    return buffer;
  }
}
