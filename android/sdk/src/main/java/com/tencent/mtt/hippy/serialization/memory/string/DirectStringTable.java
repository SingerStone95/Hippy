package com.tencent.mtt.hippy.serialization.memory.string;

import com.tencent.mtt.hippy.serialization.StringLocation;

import java.io.UnsupportedEncodingException;

public class DirectStringTable implements StringTable {
  @Override
  public String lookup(char[] chars, StringLocation location, Object relatedKey) {
    return new String(chars);
  }

  @Override
  public String lookup(byte[] bytes, String encoding, StringLocation location, Object relatedKey) throws UnsupportedEncodingException {
    return new String(bytes, encoding);
  }
}
