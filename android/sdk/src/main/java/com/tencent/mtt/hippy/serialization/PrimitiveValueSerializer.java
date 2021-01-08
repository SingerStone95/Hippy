/*
 * Tencent is pleased to support the open source community by making Hippy
 * available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.serialization;

import com.tencent.mtt.hippy.exception.UnreachableCodeException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public abstract class PrimitiveValueSerializer extends V8Serialization {
  /** Buffer used for serialization. */
  protected ByteBuffer buffer = allocateBuffer(1024);
  /** ID of the next serialized object. **/
  private int nextId;
  /** Maps a serialized object to its ID. */
  private final Map<Object, Integer> objectMap = new IdentityHashMap<>();

  protected PrimitiveValueSerializer() {
    super();
  }

  protected static ByteBuffer allocateBuffer(int capacity) {
    return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
  }

  protected void ensureFreeSpace(int spaceNeeded) {
    ByteBuffer oldBuffer = buffer;
    int capacity = oldBuffer.capacity();
    int capacityNeeded = oldBuffer.position() + spaceNeeded;
    if (capacityNeeded > capacity) {
      int newCapacity = Math.max(capacityNeeded, 2 * capacity);
      ByteBuffer newBuffer = allocateBuffer(newCapacity);
      oldBuffer.flip();
      newBuffer.put(oldBuffer);
      buffer = newBuffer;
    }
  }

  public void writeHeader() {
    ensureFreeSpace(2);
    buffer.put(VERSION);
    buffer.put(LATEST_VERSION);
  }

  protected void writeTag(SerializationTag tag) {
    writeByte(tag.getTag());
  }

  protected void writeTag(ArrayBufferViewTag tag) {
    writeByte(tag.getTag());
  }

  protected void writeTag(ErrorTag tag) {
    writeByte(tag.getTag());
  }

  protected void writeByte(byte b) {
    ensureFreeSpace(1);
    buffer.put(b);
  }

  public void writeValue(Object value) {
    if (value == Boolean.TRUE) {
      writeTag(SerializationTag.TRUE);
    } else if (value == Boolean.FALSE) {
      writeTag(SerializationTag.FALSE);
    } else if (value == Hole) {
      writeTag(SerializationTag.THE_HOLE);
    } else if (value == Undefined) {
      writeTag(SerializationTag.UNDEFINED);
    } else if (value == Null) {
      writeTag(SerializationTag.NULL);
    } else if (value instanceof Integer) {
      writeInt((Integer) value);
    } else if (value instanceof BigInteger) {
      writeTag(SerializationTag.BIG_INT);
      writeBigIntContents((BigInteger) value);
    } else if (value instanceof Number) {
      double doubleValue = ((Number) value).doubleValue();
      writeIntOrDouble(doubleValue);
    } else if (value instanceof String) {
      writeString((String) value);
    } else {
      Integer id = objectMap.get(value);
      if (id != null) {
        writeTag(SerializationTag.OBJECT_REFERENCE);
        writeVarInt(id);
      } else {
        beforeWriteObject(value);
        writeObject(value);
      }
    }
  }

  protected void beforeWriteObject(Object object) { }

  protected void writeObject(Object object) {
    assignId(object);
    if (object instanceof Date) {
      writeTag(SerializationTag.DATE);
      writeDate((Date) object);
    } else if (object instanceof Pattern) {
      writeJSRegExp((Pattern) object);
    } else {
      writeCustomObjectValue(object);
    }
  }

  protected abstract void writeCustomObjectValue(Object object);

  protected void writeInt(int value) {
    writeTag(SerializationTag.INT32);
    int zigzag = (value << 1) ^ (value >> 31);
    writeVarInt(IntegerPolyfill.toUnsignedLong(zigzag));
  }

  public void writeVarInt(long value) {
    long rest = value;
    byte[] bytes = new byte[10];
    int idx = 0;
    do {
      byte b = (byte) rest;
      b |= 0x80;
      bytes[idx] = b;
      idx++;
      rest >>>= 7;
    } while (rest != 0);
    bytes[idx - 1] &= 0x7f;
    writeBytes(bytes, idx);
  }

  protected void writeBytes(byte[] bytes, int length) {
    ensureFreeSpace(length);
    buffer.put(bytes, 0, length);
  }

  public void writeBytes(ByteBuffer bytes) {
    ensureFreeSpace(bytes.remaining());
    buffer.put(bytes);
  }

  private static boolean doubleIsInt(double value) {
    return (value == Math.floor(value)) && !Double.isInfinite(value);
  }

  public void writeIntOrDouble(double value) {
    if (doubleIsInt(value)) {
      writeInt((int) value);
    } else {
      writeTag(SerializationTag.DOUBLE);
      writeDouble(value);
    }
  }

  public void writeDouble(double value) {
    ensureFreeSpace(8);
    buffer.putDouble(value);
  }

  protected void writeString(String string) {
    try {
      byte[] bytes;
      SerializationTag tag;
      String encoding;
      if (isOneByteString(string)) {
        tag = SerializationTag.ONE_BYTE_STRING;
        encoding = "ISO-8859-1";
      } else {
        tag = SerializationTag.TWO_BYTE_STRING;
        encoding = NATIVE_UTF16_ENCODING;
      }
      writeTag(tag);
      bytes = string.getBytes(encoding);
      writeVarInt(bytes.length);
      writeBytes(bytes, bytes.length);
    } catch (UnsupportedEncodingException e) {
      throw new UnreachableCodeException();
    }
  }

  protected static boolean isOneByteString(String string) {
    for (char c : string.toCharArray()) {
      if (c >= 256) {
        return false;
      }
    }
    return true;
  }

  protected void writeBigIntContents(BigInteger bigInteger) {
    boolean negative = bigInteger.signum() == -1;
    if (negative) {
      bigInteger = bigInteger.negate();
    }
    int bitLength = bigInteger.bitLength();
    int digits = (bitLength + 63) / 64;
    int bytes = digits * 8;
    int bitfield = bytes;
    bitfield <<= 1;
    if (negative) {
      bitfield++;
    }
    writeVarInt(bitfield);
    for (int i = 0; i < bytes; i++) {
      byte b = 0;
      for (int bit = 8 * (i + 1) - 1; bit >= 8 * i; bit--) {
        b <<= 1;
        if (bigInteger.testBit(bit)) {
          b++;
        }
      }
      writeByte(b);
    }
  }

  protected void writeJSRegExp(Pattern value) {
    String pattern = value.pattern();
    int flags = value.flags();
    writeTag(SerializationTag.REGEXP);
    writeString(pattern);
    writeVarInt(flags);
  }

  protected void writeDate(Date date) {
    writeDouble(date.getTime());
  }

  public int size() {
    return buffer.position();
  }

  public void release(ByteBuffer targetBuffer) {
    buffer.flip();
    targetBuffer.put(buffer);
  }

  protected void assignId(Object object) {
    objectMap.put(object, nextId++);
  }
}
