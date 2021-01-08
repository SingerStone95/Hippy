package com.tencent.mtt.hippy.runtime;

public final class StackTrace implements Cloneable {
  private StackFrame[] frames;

  public StackTrace(StackFrame[] frames) {
    this.frames = frames;
  }

  public StackFrame getFrame(int index) {
    return frames[index];
  }

  public StackFrame[] getFrames() {
    return frames;
  }

  public int getFrameCount() {
   return frames.length;
  }

  @Override
  public StackTrace clone() throws CloneNotSupportedException {
    StackTrace clonedObject = (StackTrace) super.clone();
    StackFrame[] newFrames = new StackFrame[frames.length];
    for (int i = 0; i < frames.length; i ++) {
      newFrames[i] = frames[i].clone();
    }
    clonedObject.frames = newFrames;
    return clonedObject;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (StackFrame frame: frames) {
      stringBuilder.append("at ");
      stringBuilder.append(frame.getFunctionName());
      stringBuilder.append(" (");
      stringBuilder.append(frame.getScriptNameOrSourceURL());
      stringBuilder.append(":");
      stringBuilder.append(frame.getLineNumber());
      stringBuilder.append(":");
      stringBuilder.append(frame.getColumn());
      stringBuilder.append("\n");
    }
    int lastLFPosition = stringBuilder.length() - 1;
    if (lastLFPosition >= 0) {
      stringBuilder.deleteCharAt(lastLFPosition);
    }
    return stringBuilder.toString();
  }
}
