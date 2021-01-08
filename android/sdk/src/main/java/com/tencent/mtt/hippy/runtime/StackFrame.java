package com.tencent.mtt.hippy.runtime;

public final class StackFrame implements Cloneable {
  private final int lineNumber;
  private final int column;
  private final int scriptId;
  private final String scriptNameOrUrl;
  private final String functionName;
  private final boolean eval;
  private final boolean constructor;
  private final boolean wasm;
  private final boolean userJavascript;

  public StackFrame(int lineNumber, int column, int scriptId, String scriptNameOrUrl, String functionName, boolean eval, boolean constructor, boolean wasm, boolean userJavascript) {
    this.lineNumber = lineNumber;
    this.column = column;
    this.scriptId = scriptId;
    this.scriptNameOrUrl = scriptNameOrUrl;
    this.functionName = functionName;
    this.eval = eval;
    this.constructor = constructor;
    this.wasm = wasm;
    this.userJavascript = userJavascript;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getColumn() {
    return column;
  }

  public int getScriptId() {
    return scriptId;
  }

  public String getScriptNameOrSourceURL() {
    return scriptNameOrUrl;
  }

  public String getFunctionName() {
    return functionName;
  }

  public boolean isConstructor() {
    return constructor;
  }

  public boolean isEval() {
    return eval;
  }

  public boolean isWasm() {
    return wasm;
  }

  public boolean isUserJavascript() {
    return userJavascript;
  }

  @Override
  public StackFrame clone() throws CloneNotSupportedException {
    return (StackFrame) super.clone();
  }
}
