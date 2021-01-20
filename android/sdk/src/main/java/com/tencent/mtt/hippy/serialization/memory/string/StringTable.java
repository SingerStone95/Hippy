package com.tencent.mtt.hippy.serialization.memory.string;

import com.tencent.mtt.hippy.serialization.StringLocation;

import java.io.UnsupportedEncodingException;

public interface StringTable {
  public String lookup(char[] chars, StringLocation location, Object relatedKey);
  public String lookup(byte[] bytes, String encoding, StringLocation location, Object relatedKey) throws UnsupportedEncodingException;
  public void release();
}
