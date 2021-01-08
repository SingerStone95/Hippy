package com.tencent.mtt.hippy.runtime;

public final class JSException implements Cloneable {
  public enum ErrorLevel {
    Log,
    Debug,
    Info,
    Error,
    Warning,
    All
  }

  private final String message;
  private final ErrorLevel errorLevel;
  private final int startPosition;
  private final int endPosition;
  private final int startColumn;
  private final int endColumn;
  private final int wasmFunctionIndex;
  private final boolean sharedCrossOrigin;
  private final boolean opaque;
  private StackTrace stack;

  public JSException(String message, ErrorLevel errorLevel, StackTrace stack, int startPosition, int endPosition, int startColumn, int endColumn, int wasmFunctionIndex, boolean sharedCrossOrigin, boolean opaque) {
    this.message = message;
    this.errorLevel = errorLevel;
    this.stack = stack;
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
    this.wasmFunctionIndex = wasmFunctionIndex;
    this.sharedCrossOrigin = sharedCrossOrigin;
    this.opaque = opaque;
  }

  public String getMessage() {
    return message;
  }

  public ErrorLevel getErrorLevel() {
    return errorLevel;
  }

  public StackTrace getStack() {
    return stack;
  }

  public int getStartPosition() {
    return startPosition;
  }

  public int getEndPosition() {
    return endPosition;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public int getWasmFunctionIndex() {
    return wasmFunctionIndex;
  }

  public boolean isSharedCrossOrigin() {
    return sharedCrossOrigin;
  }

  public boolean isOpaque() {
    return opaque;
  }

  @Override
  public String toString() {
    return message + "\n" + stack.toString();
  }

  @Override
  protected JSException clone() throws CloneNotSupportedException {
    JSException clonedObject = (JSException) super.clone();
    clonedObject.stack = stack.clone();
    return clonedObject;
  }
}
