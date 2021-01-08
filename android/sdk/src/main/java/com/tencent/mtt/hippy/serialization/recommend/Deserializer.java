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
package com.tencent.mtt.hippy.serialization.recommend;

import com.tencent.mtt.hippy.exception.OutOfJavaArrayMaxSizeException;
import com.tencent.mtt.hippy.exception.OutOfJavaIntegerMaxValueException;
import com.tencent.mtt.hippy.exception.UnexpectedException;
import com.tencent.mtt.hippy.exception.UnreachableCodeException;
import com.tencent.mtt.hippy.runtime.builtins.array.JSSparseArray;
import com.tencent.mtt.hippy.serialization.ErrorTag;
import com.tencent.mtt.hippy.serialization.PrimitiveValueDeserializer;
import com.tencent.mtt.hippy.serialization.SerializationTag;
import com.tencent.mtt.hippy.runtime.builtins.array.JSDenseArray;
import com.tencent.mtt.hippy.runtime.builtins.JSArrayBuffer;
import com.tencent.mtt.hippy.runtime.builtins.JSDataView;
import com.tencent.mtt.hippy.runtime.builtins.JSError;
import com.tencent.mtt.hippy.runtime.builtins.JSMap;
import com.tencent.mtt.hippy.runtime.builtins.JSObject;
import com.tencent.mtt.hippy.runtime.builtins.JSOddball;
import com.tencent.mtt.hippy.runtime.builtins.JSSet;
import com.tencent.mtt.hippy.runtime.builtins.JSSharedArrayBuffer;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSBigintObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSBooleanObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSNumberObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSStringObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Implementation of {@code v8::(internal::)ValueDeserializer}.
 */
public class Deserializer extends PrimitiveValueDeserializer {
  /** Pointer to the corresponding v8::ValueDeserializer. */
  private final long delegate;
  /** Maps transfer ID to the transferred object. */
  private final Map<Integer, Object> transferMap = new HashMap<>();

  public Deserializer(long delegate, ByteBuffer buffer) {
    super(buffer);
    this.delegate = delegate;
  }

  @Override
  protected Object getHole() {
    return JSOddball.Hole;
  }

  @Override
  protected Object getUndefined() {
    return JSOddball.Undefined;
  }

  @Override
  protected Object getNull() {
    return JSOddball.Null;
  }

  @Override
  protected Object readJSBoolean(boolean value) {
    return assignId(value ? JSBooleanObject.True : JSBooleanObject.False);
  }

  @Override
  protected JSNumberObject readJSNumber() {
    double value = readDouble();
    return assignId(new JSNumberObject(value));
  }

  @Override
  protected JSBigintObject readJSBigInt() {
    BigInteger value = readBigInt();
    return assignId(new JSBigintObject(value));
  }

  @Override
  protected JSStringObject readJSString() {
    String value = readString();
    return assignId(new JSStringObject(value));
  }

  @Override
  protected Object readJSArrayBuffer() {
    int byteLength = readVarInt();
    if (byteLength < 0) {
      throw new OutOfJavaArrayMaxSizeException(byteLength);
    }
    JSArrayBuffer arrayBufferObject = JSArrayBuffer.allocateDirect(byteLength);
    ByteBuffer arrayBuffer = arrayBufferObject.getBuffer();
    for (int i = 0; i < byteLength; i++) {
      arrayBuffer.put(i, buffer.get());
    }
    assignId(arrayBufferObject);
    return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(arrayBufferObject) : arrayBuffer;
  }

  @Override
  protected JSObject readJSObject() {
    JSObject object = new JSObject();
    assignId(object);
    int read = readJSObjectProperties(object, SerializationTag.END_JS_OBJECT);
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of properties");
    }
    return object;
  }

  private int readJSObjectProperties(JSObject object, SerializationTag endTag) {
    SerializationTag tag;
    int count = 0;
    while ((tag = readTag()) != endTag) {
      count++;
      Object key = readValue(tag);
      if (!(key instanceof String)) {
        throw new AssertionError("Object key is not of String type");
      }
      Object value = readValue();
      object.set((String) key, value);
    }
    return count;
  }

  @Override
  protected JSMap readJSMap() {
    JSMap map = new JSMap();
    assignId(map);
    SerializationTag tag;
    int read = 0;
    HashMap<Object, Object> internalMap = map.getInternalMap();
    while ((tag = readTag()) != SerializationTag.END_JS_MAP) {
      read++;
      Object key = readValue(tag);
      Object value = readValue();
      internalMap.put(key, value);
    }
    int expected = readVarInt();
    if (2 * read != expected) {
      throw new UnexpectedException("unexpected number of entries");
    }
    return map;
  }

  @Override
  protected JSSet readJSSet() {
    JSSet set = new JSSet();
    assignId(set);
    SerializationTag tag;
    int read = 0;
    HashSet<Object> internalSet = set.getInternalSet();
    while ((tag = readTag()) != SerializationTag.END_JS_SET) {
      read++;
      Object value = readValue(tag);
      internalSet.add(value);
    }
    int expected = readVarInt();
    if (read != expected) {
      throw new UnexpectedException("unexpected number of values");
    }
    return set;
  }

  @Override
  protected JSDenseArray readDenseArray() {
    int length = readVarInt();
    if (length < 0) {
      throw new OutOfJavaArrayMaxSizeException(length);
    }
    JSDenseArray array = new JSDenseArray(length);
    assignId(array);
    for (int i = 0; i < length; i++) {
      SerializationTag tag = readTag();
      array.push(readValue(tag));
    }

    int read = readJSObjectProperties(array, SerializationTag.END_DENSE_JS_ARRAY);
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
  protected JSSparseArray readSparseArray() {
    long length = readVarLong();
    JSSparseArray array = new JSSparseArray();
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

  private JSDataView<JSArrayBuffer> readJSArrayBufferView(JSArrayBuffer arrayBuffer) {
    SerializationTag arrayBufferViewTag = readTag();
    if (arrayBufferViewTag != SerializationTag.ARRAY_BUFFER_VIEW) {
      throw new AssertionError("ArrayBufferViewTag: " + arrayBufferViewTag);
    }
    int offset = readVarInt();
    if (offset < 0) {
      throw new OutOfJavaIntegerMaxValueException(offset);
    }
    int byteLength = readVarInt();
    if (byteLength < 0) {
      throw new OutOfJavaIntegerMaxValueException(byteLength);
    }
    JSDataView.DataViewKind kind;
    switch (readArrayBufferViewTag()) {
      case DATA_VIEW: {
        kind = JSDataView.DataViewKind.DATA_VIEW;
        break;
      }
      case FLOAT32_ARRAY: {
        kind = JSDataView.DataViewKind.FLOAT32_ARRAY;
        break;
      }
      case FLOAT64_ARRAY: {
        kind = JSDataView.DataViewKind.FLOAT64_ARRAY;
        break;
      }
      case INT8_ARRAY: {
        kind = JSDataView.DataViewKind.INT8_ARRAY;
        break;
      }
      case INT16_ARRAY: {
        kind = JSDataView.DataViewKind.INT16_ARRAY;
        break;
      }
      case INT32_ARRAY: {
        kind = JSDataView.DataViewKind.INT32_ARRAY;
        break;
      }
      case UINT8_ARRAY: {
        kind = JSDataView.DataViewKind.UINT8_ARRAY;
        break;
      }
      case UINT8_CLAMPED_ARRAY: {
        kind = JSDataView.DataViewKind.UINT8_CLAMPED_ARRAY;
        break;
      }
      case UINT16_ARRAY: {
        kind = JSDataView.DataViewKind.UINT16_ARRAY;
        break;
      }
      case UINT32_ARRAY: {
        kind = JSDataView.DataViewKind.UINT32_ARRAY;
        break;
      }
      default: {
        throw new UnreachableCodeException();
      }
    }
    JSDataView<JSArrayBuffer> view = new JSDataView<>(arrayBuffer, kind, offset, byteLength);
    assignId(view);
    return view;
  }

  @Override
  protected JSError readJSError() {
    JSError.ErrorType errorType = JSError.ErrorType.Error;
    String message = null;
    String stack = null;
    boolean done = false;
    while (!done) {
      ErrorTag tag = readErrorTag();
      if (tag == null) {
        break;
      }
      switch (tag) {
        case EVAL_ERROR:
          errorType = JSError.ErrorType.EvalError;
          break;
        case RANGE_ERROR:
          errorType = JSError.ErrorType.RangeError;
          break;
        case REFERENCE_ERROR:
          errorType = JSError.ErrorType.ReferenceError;
          break;
        case SYNTAX_ERROR:
          errorType = JSError.ErrorType.SyntaxError;
          break;
        case TYPE_ERROR:
          errorType = JSError.ErrorType.TypeError;
          break;
        case URI_ERROR:
          errorType = JSError.ErrorType.URIError;
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

    JSError error = new JSError(errorType, message, stack);
    assignId(error);
    return error;
  }

  @Override
  protected Object readHostObject() {
    return assignId(readHostObject(delegate));
  }

  public void transferArrayBuffer(int id, JSArrayBuffer arrayBuffer) {
    transferMap.put(id, arrayBuffer);
  }

  @Override
  public Object readTransferredJSArrayBuffer() {
    int id = readVarInt();
    JSArrayBuffer arrayBuffer = (JSArrayBuffer) transferMap.get(id);
    if (arrayBuffer == null) {
      throw new AssertionError("Invalid transfer id " + id);
    }
    assignId(arrayBuffer);
    return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(arrayBuffer) : arrayBuffer;
  }

  @Override
  public Object readSharedArrayBuffer() {
    int id = readVarInt();
    JSSharedArrayBuffer sharedArrayBuffer = (JSSharedArrayBuffer) getSharedArrayBufferFromId(delegate, id);
    assignId(sharedArrayBuffer);
    return (peekTag() == SerializationTag.ARRAY_BUFFER_VIEW) ? readJSArrayBufferView(sharedArrayBuffer) : sharedArrayBuffer;
  }

  protected native Object getSharedArrayBufferFromId(long delegate, int id);
  protected native Object readHostObject(long delegate);
}
