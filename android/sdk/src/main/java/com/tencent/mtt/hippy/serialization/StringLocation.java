package com.tencent.mtt.hippy.serialization;

import com.tencent.mtt.hippy.runtime.builtins.JSError;

public enum StringLocation {
  /**
   * Independent Value
   */
  TOP_LEVEL,

  /**
   * Only string properties key, for {@link com.tencent.mtt.hippy.runtime.builtins.JSObject}
   */
  OBJECT_KEY,

  /**
   * Only string properties key, for {@link com.tencent.mtt.hippy.runtime.builtins.JSMap}
   */
  MAP_KEY,

  /**
   * Only string properties key, for {@link com.tencent.mtt.hippy.runtime.builtins.array.JSSparseArray}
   */
  SPARSE_ARRAY_KEY,

  /**
   * Only string properties key, for {@link com.tencent.mtt.hippy.runtime.builtins.array.JSDenseArray}
   */
  DENSE_ARRAY_KEY,

  /**
   * Related Property key is associated with {@link com.tencent.mtt.hippy.runtime.builtins.JSObject}, and its type is {@link java.lang.String}
   */
  OBJECT_VALUE,

  /**
   * Related Property key is associated with {@link com.tencent.mtt.hippy.runtime.builtins.JSMap}, and its type is {@link java.lang.Object}
   */
  MAP_VALUE,

  /**
   * Related Property key is associated with {@link com.tencent.mtt.hippy.runtime.builtins.array.JSSparseArray}, and its type is {@link java.lang.Integer} if properties, {@link java.lang.String} if elements
   */
  SPARSE_ARRAY_ITEM,

  /**
   * Related Property key is associated with {@link com.tencent.mtt.hippy.runtime.builtins.array.JSDenseArray}, and its type is {@link java.lang.Integer} if properties, {@link java.lang.String} if elements
   */
  DENSE_ARRAY_ITEM,

  /**
   * String for {@link com.tencent.mtt.hippy.runtime.builtins.JSSet} item
   */
  SET_ITEM,

  /**
   * String for {@link JSError#getMessage()}
   */
  ERROR_MESSAGE,

  /**
   * String for {@link JSError#getStack()}
   */
  ERROR_STACK,

  /**
   * String for compiles the given regular expression into a {@link java.util.regex.Pattern}
   */
  REGEXP,

  /**
   * String is not used in anywhere
   */
  VOID
}
