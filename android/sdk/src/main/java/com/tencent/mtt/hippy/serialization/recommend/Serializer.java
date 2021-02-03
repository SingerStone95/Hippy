/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.serialization.recommend;

import android.util.Pair;

import com.tencent.mtt.hippy.exception.UnreachableCodeException;
import com.tencent.mtt.hippy.runtime.builtins.JSRegExp;
import com.tencent.mtt.hippy.runtime.builtins.JSValue;
import com.tencent.mtt.hippy.runtime.builtins.array.JSAbstractArray;
import com.tencent.mtt.hippy.runtime.builtins.array.JSSparseArray;
import com.tencent.mtt.hippy.runtime.builtins.wasm.WasmModule;
import com.tencent.mtt.hippy.serialization.ArrayBufferViewTag;
import com.tencent.mtt.hippy.serialization.exception.DataCloneException;
import com.tencent.mtt.hippy.serialization.ErrorTag;
import com.tencent.mtt.hippy.serialization.utils.IntegerPolyfill;
import com.tencent.mtt.hippy.serialization.PrimitiveValueSerializer;
import com.tencent.mtt.hippy.serialization.SerializationTag;
import com.tencent.mtt.hippy.runtime.builtins.JSArrayBuffer;
import com.tencent.mtt.hippy.runtime.builtins.JSError;
import com.tencent.mtt.hippy.runtime.builtins.JSMap;
import com.tencent.mtt.hippy.runtime.builtins.JSObject;
import com.tencent.mtt.hippy.runtime.builtins.JSSet;
import com.tencent.mtt.hippy.runtime.builtins.JSSharedArrayBuffer;
import com.tencent.mtt.hippy.runtime.builtins.JSDataView;
import com.tencent.mtt.hippy.runtime.builtins.JSOddball;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSBigintObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSBooleanObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSNumberObject;
import com.tencent.mtt.hippy.runtime.builtins.objects.JSStringObject;
import com.tencent.mtt.hippy.serialization.memory.buffer.Allocator;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public class Serializer extends PrimitiveValueSerializer {
  public interface Delegate {
    /**
     * Implement this method to write some kind of host object,
     * if possible. If not,
     * a {@link DataCloneException} exception should be thrown.
     *
     * @param serializer current serializer
     * @param object host object
     */
    void writeHostObject(Serializer serializer, Object object);

    /**
     * Called when the Serializer is going to serialize a {@link JSSharedArrayBuffer} object.
     * Implement must return an ID for the object,
     * using the same ID if this {@link JSSharedArrayBuffer} has already been serialized in this buffer.
     * <br/>
     * When deserializing, this ID will be passed to Deserializer.Delegate#getSharedArrayBufferFromId as |clone_id|.
     * <br/>
     * If the object cannot be serialized, an exception {@link DataCloneException} should be thrown.
     *
     * @param serializer current serializer
     * @param sharedArrayBuffer SharedArrayBuffer
     * @return ID
     */
    int getSharedArrayBufferId(Serializer serializer, JSSharedArrayBuffer sharedArrayBuffer);

    /**
     * Called when the Serializer is going to serialize a {@link WasmModule} object.
     * Implement must return an ID for the object,
     * using the same ID if this {@link WasmModule} has already been serialized in this buffer.
     * <br/>
     * When deserializing, this ID will be passed to Deserializer.Delegate#getWasmModuleFromId as |transfer_id|.
     * <br/>
     * If the object cannot be serialized, an exception {@link DataCloneException} should be thrown.
     *
     * @param serializer current serializer
     * @param module WebAssembly Module
     * @return ID
     */
    int getWasmModuleTransferId(Serializer serializer, WasmModule module);
  }

  /** Implement for Delegate interface */
  private final Delegate delegate;
  /** Maps a transferred {@link JSArrayBuffer} to its transfer ID. */
  private Map<JSArrayBuffer, Integer> arrayBufferTransferMap;
  /** Determines whether {@link JSArrayBuffer}s should be serialized as host objects. */
  private boolean treatArrayBufferViewsAsHostObjects;

  public Serializer() {
    this(null, null);
  }

  public Serializer(Delegate delegate) {
    this(null, delegate);
  }

  public Serializer(Allocator<ByteBuffer> allocator) {
    this(allocator, null);
  }

  public Serializer(Allocator<ByteBuffer> allocator, Delegate delegate) {
    super(allocator);
    this.delegate = delegate;
  }

  /**
   * Indicate whether to treat {@link JSDataView} objects as host objects,
   * i.e. pass them to Delegate#WriteHostObject. This should not be called when no Delegate was passed.
   * <br/>
   * The default is not to treat ArrayBufferViews as host objects.
   *
   * @param mode treat mode
   */
  public void setTreatArrayBufferViewsAsHostObjects(boolean mode) {
    ensureNotReleased();
    treatArrayBufferViewsAsHostObjects = mode;
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
  protected void beforeWriteObject(Object object) {
    if (!treatArrayBufferViewsAsHostObjects && JSValue.is(object) && ((JSValue) object).isDataView()) {
      JSDataView<?> view = (JSDataView<?>) object;
      assignId(view);
      if (view.getBufferObject() instanceof JSArrayBuffer) {
        writeJSSharedArrayBuffer((JSSharedArrayBuffer) view.getBufferObject());
      } else {
        writeJSArrayBuffer(view.getBufferObject());
      }
    }
  }

  @Override
  protected void writeCustomObjectValue(Object object) {
    if (JSValue.is(object)) {
      JSValue value = (JSValue) object;

      if (value.isArray()) {
        writeJSArray((JSAbstractArray) value);
      } else if (value.isDataView()) {
        writeJSArrayBufferView((JSDataView<?>) value);
      } else if (value.isError()) {
        writeJSError((JSError) value);
      } else if (value.isRegExp()) {
        writeJSRegExp((JSRegExp) value);
      } else if (value.isObject()) {
        writeJSObject((JSObject) value);
      } else if (value.isMap()) {
        writeJSMap((JSMap) value);
      } else if (value.isSet()) {
        writeJSSet((JSSet) value);
      } else if (value.isSharedArrayBuffer()) {
        writeJSSharedArrayBuffer((JSSharedArrayBuffer) value);
      } else if (value.isArrayBuffer()) {
        writeJSArrayBuffer((JSArrayBuffer) value);
      } else if (value.isBooleanObject()) {
        writeJSBoolean((JSBooleanObject) value);
      } else if (value.isBigIntObject()) {
        writeTag(SerializationTag.BIG_INT_OBJECT);
        writeJSBigIntContents((JSBigintObject) value);
      } else if (value.isNumberObject()) {
        writeJSNumber((JSNumberObject) value);
      } else if (value.isStringObject()) {
        writeJSString((JSStringObject) value);
      } else {
        throw new UnreachableCodeException();
      }
    } else {
      writeHostObject(object);
    }
  }

  private void writeJSBoolean(JSBooleanObject value) {
    writeTag(value.isTrue() ? SerializationTag.TRUE_OBJECT : SerializationTag.FALSE_OBJECT);
  }

  private void writeJSBigIntContents(JSBigintObject value) {
    writeBigIntContents(value.getValue());
  }

  private void writeJSNumber(JSNumberObject value) {
    writeTag(SerializationTag.NUMBER_OBJECT);
    writeDouble(value.getValue().doubleValue());
  }

  private void writeJSString(JSStringObject value) {
    writeTag(SerializationTag.STRING_OBJECT);
    writeString(value.getValue().toString());
  }

  private void writeJSRegExp(JSRegExp value) {
    writeTag(SerializationTag.REGEXP);
    writeString(value.getSource());
    writeVarIntOrLong(value.getFlags());
  }

  private void writeJSArrayBuffer(JSArrayBuffer value) {
    if (arrayBufferTransferMap == null) {
      arrayBufferTransferMap = new IdentityHashMap<>();
    }

    Integer id = arrayBufferTransferMap.get(value);
    if (id == null) {
      ByteBuffer source = value.getBuffer();
      int byteLength = source.capacity();
      writeTag(SerializationTag.ARRAY_BUFFER);
      writeVarIntOrLong(byteLength);
      ensureFreeSpace(byteLength);
      for (int i = 0; i < byteLength; i++) {
        buffer.put(source.get(i));
      }
    } else {
      writeTag(SerializationTag.ARRAY_BUFFER_TRANSFER);
      writeVarIntOrLong(IntegerPolyfill.toUnsignedLong(id));
    }
  }

  private void writeJSSharedArrayBuffer(JSSharedArrayBuffer value) {
    if (delegate == null) {
      throw new DataCloneException(value);
    }
    int id = delegate.getSharedArrayBufferId(this, value);
    writeTag(SerializationTag.SHARED_ARRAY_BUFFER);
    writeVarIntOrLong(id);
  }

  private void writeJSObject(JSObject value) {
    writeTag(SerializationTag.BEGIN_JS_OBJECT);
    writeJSObjectProperties(value.entries());
    writeTag(SerializationTag.END_JS_OBJECT);
    writeVarIntOrLong(value.size());
  }

  private void writeJSObjectProperties (Set<Pair<String, Object>> props) {
    for (Pair<String, Object> prop : props) {
      writeString(prop.first);
      writeValue(prop.second);
    }
  }

  private void writeJSMap(JSMap value) {
    writeTag(SerializationTag.BEGIN_JS_MAP);
    Iterator<Map.Entry<Object, Object>> entries = value.getInternalMap().entrySet().iterator();
    int count = 0;
    while (entries.hasNext()) {
      count++;
      Map.Entry<Object, Object> entry = entries.next();
      writeValue(entry.getKey());
      writeValue(entry.getValue());
    }
    writeTag(SerializationTag.END_JS_MAP);
    writeVarIntOrLong(2 * count);
  }

  private void writeJSSet(JSSet value) {
    writeTag(SerializationTag.BEGIN_JS_SET);
    Iterator<Object> entries = value.getInternalSet().iterator();
    int count = 0;
    while (entries.hasNext()) {
      count++;
      writeValue(entries.next());
    }
    writeTag(SerializationTag.END_JS_SET);
    writeVarIntOrLong(count);
  }

  private void writeJSArray(JSAbstractArray value) {
    int length = value.size();
    if (value.isDenseArray()) {
      writeTag(SerializationTag.BEGIN_DENSE_JS_ARRAY);
      writeVarIntOrLong(length);
      for (int i = 0; i < length; i++) {
        writeValue(value.get(i));
      }
      writeJSObjectProperties(JSObject.entries(value));
      writeTag(SerializationTag.END_DENSE_JS_ARRAY);
    } else if (value.isSparseArray()) {
      writeTag(SerializationTag.BEGIN_SPARSE_JS_ARRAY);
      writeVarIntOrLong(length);
      for (Pair<Integer, Object> item: ((JSSparseArray) value).items()) {
        writeInt(item.first);
        writeValue(item.second);
      }
      writeJSObjectProperties(JSObject.entries(value));
      writeTag(SerializationTag.END_SPARSE_JS_ARRAY);
    } else {
      throw new UnreachableCodeException();
    }
    writeVarIntOrLong(JSObject.size(value));
    writeVarIntOrLong(length);
  }

  private void writeJSArrayBufferView(JSDataView<?> value) {
    if (treatArrayBufferViewsAsHostObjects) {
      writeHostObject(value);
    } else {
      writeTag(SerializationTag.ARRAY_BUFFER_VIEW);
      ArrayBufferViewTag tag;
      switch (value.getKind()) {
        case DATA_VIEW: {
          tag = ArrayBufferViewTag.DATA_VIEW;
          break;
        }
        case FLOAT32_ARRAY: {
          tag = ArrayBufferViewTag.FLOAT32_ARRAY;
          break;
        }
        case FLOAT64_ARRAY: {
          tag = ArrayBufferViewTag.FLOAT64_ARRAY;
          break;
        }
        case INT8_ARRAY: {
          tag = ArrayBufferViewTag.INT8_ARRAY;
          break;
        }
        case INT16_ARRAY: {
          tag = ArrayBufferViewTag.INT16_ARRAY;
          break;
        }
        case INT32_ARRAY: {
          tag = ArrayBufferViewTag.INT32_ARRAY;
          break;
        }
        case UINT8_ARRAY: {
          tag = ArrayBufferViewTag.UINT8_ARRAY;
          break;
        }
        case UINT8_CLAMPED_ARRAY: {
          tag = ArrayBufferViewTag.UINT8_CLAMPED_ARRAY;
          break;
        }
        case UINT16_ARRAY: {
          tag = ArrayBufferViewTag.UINT16_ARRAY;
          break;
        }
        case UINT32_ARRAY: {
          tag = ArrayBufferViewTag.UINT32_ARRAY;
          break;
        }
        default: {
          throw new UnreachableCodeException();
        }
      }
      writeTag(tag);
      writeVarIntOrLong(value.getByteOffset());
      writeVarIntOrLong(value.getByteLength());
    }
  }

  private void writeJSError(JSError error) {
    writeTag(SerializationTag.ERROR);
    writeErrorTypeTag(error);

    String message = error.getMessage();
    if (!message.isEmpty()) {
      writeTag(ErrorTag.MESSAGE);
      writeString(message);
    }

    String stack = error.getStack();
    if (!stack.isEmpty()) {
      writeTag(ErrorTag.STACK);
      writeString(stack);
    }

    writeTag(ErrorTag.END);
  }

  private void writeErrorTypeTag(JSError error) {
    JSError.ErrorType errorType = error.getType();
    ErrorTag tag;
    switch (errorType) {
      case EvalError:
        tag = ErrorTag.EVAL_ERROR;
        break;
      case RangeError:
        tag = ErrorTag.RANGE_ERROR;
        break;
      case ReferenceError:
        tag = ErrorTag.REFERENCE_ERROR;
        break;
      case SyntaxError:
        tag = ErrorTag.SYNTAX_ERROR;
        break;
      case TypeError:
        tag = ErrorTag.TYPE_ERROR;
        break;
      case URIError:
        tag = ErrorTag.URI_ERROR;
        break;
      default:
        tag = null;
        if (errorType != JSError.ErrorType.Error && errorType != JSError.ErrorType.AggregateError) {
          throw new UnreachableCodeException();
        }
        break;
    }
    if (tag != null) {
      writeTag(tag);
    }
  }

  private void writeHostObject(Object object) {
    writeTag(SerializationTag.HOST_OBJECT);
    if (delegate == null) {
      throw new DataCloneException(object);
    }
    delegate.writeHostObject(this, object);
  }

  /**
   * Marks an {@link JSArrayBuffer} as having its contents transferred out of band.
   * Pass the corresponding {@link JSArrayBuffer} in the deserializing context to {@link Deserializer#transferArrayBuffer(int, JSArrayBuffer)}.
   *
   * @param transferId transfer id
   * @param arrayBuffer JSArrayBuffer
   */
  public void transferArrayBuffer(int transferId, JSArrayBuffer arrayBuffer) {
    ensureNotReleased();
    if (arrayBufferTransferMap == null) {
      arrayBufferTransferMap = new IdentityHashMap<>();
    }
    arrayBufferTransferMap.put(arrayBuffer, transferId);
  }
}
