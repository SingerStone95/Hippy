package com.tencent.mtt.hippy.runtime.builtins;

public final class JSOddball extends JSValue {
  @Override
  public final Object dump() {
    return null;
  }

  @Override
  @SuppressWarnings("all") // "CloneDoesntCallSuperClone" not recognized
  protected Object clone() {
    return this;
  }

  public enum kindType {
    Hole,
    Undefined,
    Null
  }
  public static final JSOddball Hole = new JSOddball(kindType.Hole);
  public static final JSOddball Undefined = new JSOddball(kindType.Undefined);
  public static final JSOddball Null = new JSOddball(kindType.Null);

  private final kindType kind;

  private JSOddball(kindType kind) {
    this.kind = kind;
  }

  public final boolean isUndefined() {
    return kind == kindType.Undefined;
  }

  public final boolean isNull() {
    return kind == kindType.Null;
  }

  public final boolean isHole() {
    return kind == kindType.Hole;
  }
}
