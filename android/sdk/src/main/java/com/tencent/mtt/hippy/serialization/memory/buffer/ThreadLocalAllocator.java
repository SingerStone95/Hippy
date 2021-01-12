package com.tencent.mtt.hippy.serialization.memory.buffer;

import java.nio.ByteBuffer;

public class ThreadLocalAllocator extends SimpleAllocator {
  private ByteBuffer reusedBuffer;
  private int maxCacheSize = 16 * 1024; // 16k

  public ThreadLocalAllocator(int maxCacheSize) {
    this.maxCacheSize = maxCacheSize;
  }

  @Override
  public ByteBuffer allocate(int capacity) {
    if (reusedBuffer != null) {
      if (reusedBuffer.capacity() >= capacity) {
        ByteBuffer buffer = reusedBuffer;
        reusedBuffer = null;
        buffer.clear();
        return buffer;
      }
    }

    return super.allocate(capacity);
  }

  @Override
  public ByteBuffer release(ByteBuffer buffer) {
    int capacity = buffer.capacity();
    if (capacity <= maxCacheSize && (reusedBuffer == null || reusedBuffer.capacity() < capacity)) {
      reusedBuffer = buffer;
    }
    return super.release(buffer);
  }
}
