package com.tencent.mtt.hippy.runtime.builtins;

import com.tencent.mtt.hippy.runtime.StackTrace;

public class JSError extends JSObject {
  public enum ErrorType {
    Error,

    /**
     * Currently not in use, only there for compatibility with previous versions of the
     * specification ECMA262[15.11.6.1].
     */
    EvalError,

    /**
     * Indicates a numeric value has exceeded the allowable range ECMA262[15.11.6.2].
     */
    RangeError,

    /**
     * Indicate that an invalid reference value has been detected ECMA262[15.11.6.3].
     */
    ReferenceError,

    /**
     * Indicates that a parsing error has occurred ECMA262[15.11.6.4].
     */
    SyntaxError,

    /**
     * Indicates the actual type of an operand is different than the expected type
     * ECMA262[15.11.6.5].
     */
    TypeError,

    /**
     * Indicates that one of the global URI handling functions was used in a way that is
     * incompatible with its definition ECMA262[15.11.6.6].
     */
    URIError,

    AggregateError;
  }

  private StackTrace stack;
  private final String MESSAGE_NAME = "message";
  private final String STACK_NAME = "stack";
  private final String TYPE_NAME = "type";

  public JSError(ErrorType type, String message, StackTrace stack) {
    set(MESSAGE_NAME, message);
    set(TYPE_NAME, type);
    this.stack = stack;
  }

  public JSError(ErrorType type, String message, String stack) {
    set(MESSAGE_NAME, message);
    set(STACK_NAME, stack);
    set(TYPE_NAME, type);
  }

  public ErrorType getType() {
    return (ErrorType) get(TYPE_NAME);
  }

  public String getMessage() {
    return (String) get(MESSAGE_NAME);
  }

  public String getStack() {
    if (!has(STACK_NAME)) {
      set(STACK_NAME, get(MESSAGE_NAME) + "\n" + this.stack.toString());
    }
    return (String) get(STACK_NAME);
  }

  public StackTrace getStackTrace() {
    return stack;
  }

  @Override
  public JSError clone() throws CloneNotSupportedException {
    JSError clonedObject = (JSError) super.clone();
    clonedObject.stack = stack.clone();
    return clonedObject;
  }
}
