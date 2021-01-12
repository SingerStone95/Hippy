package com.tencent.mtt.hippy.serialization.utils;

import java.util.regex.Pattern;

public final class RegExpConverter {
  private RegExpConverter() {

  }

  // region JS RegExp Flags
  public static final int JS_GLOBAL = 1; // kGlobal - NOT supported in Java
  public static final int JS_IGNORE_CASE = 2; // kIgnoreCase
  public static final int JS_MULTILINE = 4; // kMultiline
  public static final int JS_STICKY = 8; // // kSticky - NOT supported in Java
  public static final int JS_UNICODE = 16; // kUnicode - always enable in Android
  public static final int JS_DOTALL = 32; // kDotAll
  // endregion

  public static int jsFlagsToPattern(int flags) {
    int patternFlags = 0;

    if ((flags & JS_IGNORE_CASE) == 0) {
      patternFlags |= Pattern.CASE_INSENSITIVE;
    }
    if ((flags & JS_MULTILINE) != 0) {
      patternFlags |= Pattern.MULTILINE;
    }
    if ((flags & JS_DOTALL) != 0) {
      patternFlags |= Pattern.DOTALL;
    }

    return patternFlags;
  }

  public static int patternFlagsToJS(int flags) {
    int jsFlags = 0;

    if ((flags & Pattern.CASE_INSENSITIVE) == 0) {
      jsFlags |= JS_IGNORE_CASE;
    }
    if ((flags & Pattern.MULTILINE) != 0) {
      jsFlags |= Pattern.MULTILINE;
    }
    if ((flags & Pattern.DOTALL) != 0) {
      jsFlags |= Pattern.DOTALL;
    }

    return jsFlags;
  }
}
