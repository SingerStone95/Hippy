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
package com.tencent.mtt.hippy.serialization.compatible;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.exception.UnexpectedException;
import com.tencent.mtt.hippy.serialization.ErrorTag;
import com.tencent.mtt.hippy.serialization.PrimitiveValueDeserializer;
import com.tencent.mtt.hippy.serialization.SerializationTag;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Compatible with {@code com.tencent.mtt.hippy.common.HippyMap}
 */
@SuppressWarnings("deprecation")
public class Deserializer extends PrimitiveValueDeserializer {
  public Deserializer(ByteBuffer buffer) {
    super(buffer);
  }

  @Override
  protected Object getUndefined() {
    return ConstantValue.Undefined;
  }

  @Override
  protected Object getNull() {
    return ConstantValue.Null;
  }

  @Override
  protected Object getHole() {
    return ConstantValue.Hole;
  }

  @Override
  protected Boolean readJSBoolean(boolean value) {
    return assignId(value);
  }

  @Override
  protected Number readJSNumber() {
    return assignId(readDouble());
  }

  @Override
  protected BigInteger readJSBigInt() {
    return assignId(readBigInt());
  }

  @Override
  protected String readJSString() {
    return assignId(readString());
  }

  @Override
  protected Object readJSArrayBuffer() {
    int byteLength = readVarInt();
    buffer.position(buffer.position() + byteLength);

    assignId(null);
    if (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) {
      readJSArrayBufferView();
    }

    return null;
  }

  @Override
  protected HippyMap readJSObject() {
    HippyMap object = new HippyMap();
    assignId(object);
    int read = readJSObjectProperties(object, SerializationTag.END_JS_OBJECT);
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of properties");
    }
    return object;
  }

  private int readJSObjectProperties(HippyMap object, SerializationTag endTag) {
    SerializationTag tag;
    int count = 0;
    while ((tag = readTag()) != endTag) {
      count++;
      Object key = readValue(tag);
      if (!(key instanceof String)) {
        throw new AssertionError("Object key is not of String type");
      }
      Object value = readValue();
      if (object != null) {
        object.pushObject((String) key, value);
      }
    }
    return count;
  }

  @Override
  protected HippyMap readJSMap() {
    HippyMap object = new HippyMap();
    assignId(object);
    SerializationTag tag;
    int read = 0;
    while ((tag = readTag()) != SerializationTag.END_JS_MAP) {
      read++;
      Object key = readValue(tag);
      Object value = readValue();
      object.pushObject(key.toString(), value);
    }
    int expected = readVarInt();
    if (2 * read != expected) {
      throw new UnexpectedException("unexpected number of entries");
    }
    return object;
  }

  @Override
  protected HippyArray readJSSet() {
    HippyArray array = new HippyArray();
    assignId(array);
    SerializationTag tag;
    int read = 0;
    while ((tag = readTag()) != SerializationTag.END_JS_SET) {
      read++;
      Object value = readValue(tag);
      array.pushObject(value);
    }
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of values");
    }
    return array;
  }

  @Override
  protected HippyArray readDenseArray() {
    int length = readVarInt();
    HippyArray array = new HippyArray();
    assignId(array);
    for (int i = 0; i < length; i++) {
      SerializationTag tag = readTag();
      if (tag != SerializationTag.THE_HOLE) {
        array.pushObject(readValue(tag));
      }
    }

    int read = readJSObjectProperties(null, SerializationTag.END_DENSE_JS_ARRAY);
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of properties");
    }
    int length2 = readVarInt();
    if (length != length2) {
      throw new AssertionError("length ambiguity");
    }
    return array;
  }

  @Override
  protected HippyMap readSparseArray() {
    long length = readVarLong();
    HippyMap array = new HippyMap();
    assignId(array);
    int read = readJSObjectProperties(array, SerializationTag.END_SPARSE_JS_ARRAY);
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of properties");
    }
    long length2 = readVarLong();
    if (length != length2) {
      throw new AssertionError("length ambiguity");
    }
    return array;
  }

  private void readJSArrayBufferView() {
    SerializationTag arrayBufferViewTag = readTag();
    if (arrayBufferViewTag != SerializationTag.ARRAY_BUFFER_VIEW) {
      throw new AssertionError("ArrayBufferViewTag: " + arrayBufferViewTag);
    }
    readVarInt();
    readVarInt();
    readArrayBufferViewTag();

    assignId(null);
  }

  @Override
  protected HippyMap readJSError() {
    String message = null;
    String stack = null;
    String errorType = null;
    boolean done = false;
    while (!done) {
      ErrorTag tag = readErrorTag();
      if (tag == null) {
        break;
      }
      switch (tag) {
        case EVAL_ERROR:
          errorType = "EvalError";
          break;
        case RANGE_ERROR:
          errorType = "RangeError";
          break;
        case REFERENCE_ERROR:
          errorType = "ReferenceError";
          break;
        case SYNTAX_ERROR:
          errorType = "SyntaxError";
          break;
        case TYPE_ERROR:
          errorType = "TypeError";
          break;
        case URI_ERROR:
          errorType = "URIError";
          break;
        case MESSAGE:
          message = readString();
          break;
        case STACK:
          stack = readString();
          break;
        default:
          if (!(tag == ErrorTag.END)) {
            throw new AssertionError("ErrorTag: " + tag);
          }
          done = true;
          break;
      }
    }

    HippyMap error = new HippyMap();
    error.pushString("message", message);
    error.pushString("stack", stack);
    error.pushString("type", errorType);
    assignId(error);
    return error;
  }

  @Override
  protected Object readHostObject() {
    return assignId(null);
  }

  @Override
  public Object readTransferredJSArrayBuffer() {
    readVarInt();
    assignId(null);
    if (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) {
      readJSArrayBufferView();
    }
    return null;
  }

  @Override
  public Object readSharedArrayBuffer() {
    readVarInt();
    assignId(null);
    if (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) {
      readJSArrayBufferView();
    }
    return null;
  }
}
