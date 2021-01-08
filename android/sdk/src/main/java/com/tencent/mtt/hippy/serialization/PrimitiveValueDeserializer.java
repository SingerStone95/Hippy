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

import com.tencent.mtt.hippy.exception.OutOfJavaArrayMaxSizeException;
import com.tencent.mtt.hippy.exception.UnreachableCodeException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of {@code v8::(internal::)ValueDeserializer}.
 */
public abstract class PrimitiveValueDeserializer extends V8Serialization {
  /** Buffer used for serialization. */
  protected final ByteBuffer buffer;
  /** Version of the data format used during serialization. */
  private int version;
  /** ID of the next deserialized object. */
  private int nextId;
  /** Maps ID of a deserialized object to the object itself. */
  private final Map<Integer, Object> objectMap = new HashMap<>();

  protected PrimitiveValueDeserializer(ByteBuffer buffer) {
    super();
    this.buffer = buffer.order(ByteOrder.nativeOrder());
  }

  public void readHeader() {
    if (buffer.get() == VERSION) {
      version = buffer.get();
      if (version > LATEST_VERSION) {
        throw new UnsupportedOperationException("Unable to deserialize cloned data due to invalid or unsupported version.");
      }
    }
  }

  public Object readValue() {
    SerializationTag tag = readTag();
    return readValue(tag);
  }

  protected Object readValue(SerializationTag tag) {
    switch (tag) {
      case TRUE:
        return Boolean.TRUE;
      case FALSE:
        return Boolean.FALSE;
      case THE_HOLE:
        return Hole;
      case UNDEFINED:
        return Undefined;
      case NULL:
        return Null;
      case INT32:
        return readInt();
      case UINT32:
        return readVarInt();
      case DOUBLE:
        return readDouble();
      case BIG_INT:
        return readBigInt();
      case ONE_BYTE_STRING:
        return readOneByteString();
      case TWO_BYTE_STRING:
        return readTwoByteString();
      case UTF8_STRING:
        return readUTF8String();
      case DATE:
        return readDate();
      case TRUE_OBJECT:
        return readJSBoolean(true);
      case FALSE_OBJECT:
        return readJSBoolean(false);
      case NUMBER_OBJECT:
        return readJSNumber();
      case BIG_INT_OBJECT:
        return readJSBigInt();
      case STRING_OBJECT:
        return readJSString();
      case REGEXP:
        return readJSRegExp();
      case ARRAY_BUFFER:
        return readJSArrayBuffer();
      case ARRAY_BUFFER_TRANSFER:
        return readTransferredJSArrayBuffer();
      case SHARED_ARRAY_BUFFER:
        return readSharedArrayBuffer();
      case BEGIN_JS_OBJECT:
        return readJSObject();
      case BEGIN_JS_MAP:
        return readJSMap();
      case BEGIN_JS_SET:
        return readJSSet();
      case BEGIN_DENSE_JS_ARRAY:
        return readDenseArray();
      case BEGIN_SPARSE_JS_ARRAY:
        return readSparseArray();
      case OBJECT_REFERENCE:
        return readObjectReference();
      case HOST_OBJECT:
        return readHostObject();
      case ERROR:
        return readJSError();
      default:
        throw new UnsupportedTagException(tag);
    }
  }

  protected SerializationTag readTag() {
    SerializationTag tag;
    do {
      tag = SerializationTag.fromTag(buffer.get());
    } while (tag == SerializationTag.PADDING);
    return tag;
  }

  protected SerializationTag peekTag() {
    return buffer.hasRemaining() ? SerializationTag.fromTag(buffer.get(buffer.position())) : null;
  }

  protected ArrayBufferViewTag readArrayBufferViewTag() {
    return ArrayBufferViewTag.fromTag(buffer.get());
  }

  protected ErrorTag readErrorTag() {
    return ErrorTag.fromTag(buffer.get());
  }

  protected int readInt() {
    long zigzag = readVarLong();
    long value = (zigzag >> 1) ^ -(zigzag & 1);
    return (int) value;
  }

  public int readVarInt() {
    return (int) readVarLong();
  }

  public long readVarLong() {
    long value = 0;
    int shift = 0;
    byte b;
    do {
      b = buffer.get();
      value |= (b & 0x7fL) << shift;
      shift += 7;
    } while ((b & 0x80) != 0);
    return value;
  }

  public double readDouble() {
    return buffer.getDouble();
  }

  protected String readString() {
    SerializationTag tag = readTag();
    switch (tag) {
      case ONE_BYTE_STRING:
        return readOneByteString();
      case TWO_BYTE_STRING:
        return readTwoByteString();
      case UTF8_STRING:
        return readUTF8String();
      default:
        throw new UnreachableCodeException();
    }
  }

  protected BigInteger readBigInt() {
    int bitField = readVarInt();
    boolean negative = (bitField & 1) != 0;
    bitField >>= 1;
    BigInteger bigInteger = BigInteger.ZERO;
    for (int i = 0; i < bitField; i++) {
      byte b = buffer.get();
      for (int bit = 8 * i; bit < 8 * (i + 1); bit++) {
        if ((b & 1) != 0) {
          bigInteger = bigInteger.setBit(bit);
        }
        b >>>= 1;
      }
    }
    if (negative) {
      bigInteger = bigInteger.negate();
    }
    return bigInteger;
  }

  protected String readOneByteString() {
    int charCount = readVarInt();
    if (charCount < 0) {
      throw new OutOfJavaArrayMaxSizeException(charCount);
    }
    char[] chars = new char[charCount];
    for (int i = 0; i < charCount; i++) {
      byte b = buffer.get();
      chars[i] = (char) (b & 0xff);
    }
    return new String(chars);
  }

  protected String readTwoByteString() {
    return readString(NATIVE_UTF16_ENCODING);
  }

  protected String readUTF8String() {
    return readString("UTF-8");
  }

  protected String readString(String encoding) {
    int byteCount = readVarInt();
    if (byteCount < 0) {
      throw new OutOfJavaArrayMaxSizeException(byteCount);
    }
    byte[] bytes = new byte[byteCount];
    buffer.get(bytes);
    try {
      return new String(bytes, encoding);
    } catch (UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }

  protected Date readDate() {
    double millis = readDouble();
    return assignId(new Date((long) millis)); // TODO：丢失精度？
  }

  protected Pattern readJSRegExp() {
    String pattern = readString();
    int flags = readVarInt();
    return assignId(Pattern.compile(pattern, flags)); // TODO: flags 是否一致？
  }

  protected Object readObjectReference() {
    int id = readVarInt();
    Object object = objectMap.get(id);
    if (object == null) {
      throw new AssertionError("invalid object reference");
    }
    return object;
  }

  protected abstract Object readJSBoolean(boolean value);
  protected abstract Object readJSNumber();
  protected abstract Object readJSBigInt();
  protected abstract Object readJSString();
  protected abstract Object readJSArrayBuffer();
  protected abstract Object readJSObject();
  protected abstract Object readJSMap();
  protected abstract Object readJSSet();
  protected abstract Object readDenseArray();
  protected abstract Object readSparseArray();
  protected abstract Object readJSError();
  protected abstract Object readHostObject();
  protected abstract Object readTransferredJSArrayBuffer();
  protected abstract Object readSharedArrayBuffer();

  public int readBytes(int length) {
    int position = buffer.position();
    buffer.position(position + length);
    return position;
  }

  protected <T> T assignId(T object) {
    objectMap.put(nextId++, object);
    return object;
  }

  public int getWireFormatVersion() {
    return version;
  }
}
