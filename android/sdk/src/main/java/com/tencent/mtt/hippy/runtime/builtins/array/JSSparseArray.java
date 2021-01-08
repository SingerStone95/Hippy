package com.tencent.mtt.hippy.runtime.builtins.array;

import android.util.Pair;
import android.util.SparseArray;

import com.tencent.mtt.hippy.runtime.builtins.JSValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class JSSparseArray extends JSAbstractArray {
  private Set<String> keySet;
  private Collection<Object> valueCollection;
  private Set<Pair<String, Object>> entrySet;
  private Set<Pair<Integer, Object>> itemSet;
  private SparseArray<Object> elements; // Maybe use LongSparseArray<T>

  public JSSparseArray() {
    this(10);
  }
  public JSSparseArray(int initialSize) {
    this.elements = new SparseArray<>(initialSize);
  }

  // region op
  @Override
  public Object get(int index) {
    return elements.get(index);
  }

  @Override
  public void push(Object value) {
    elements.append(elements.size(), value);
  }

  @Override
  public void add(int index, Object value) {
    elements.put(index, value);
  }

  @Override
  public Object set(int index, Object value) {
    add(index, value);
    return value;
  }

  @Override
  public Object delete(int index) {
    Object value = get(index);
    elements.delete(index);
    return value;
  }

  @Override
  public int size() {
    return elements.keyAt(elements.size() - 1);
  }

  private int fieldCount() {
    return elements.size() + super.size();
  }
  // endregion

  // region keys
  @Override
  public Set<String> keys() {
    Set<String> ks;
    return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
  }

  private final class KeyIterator implements Iterator<String> {
    private int currentIndex = 0;
    private final Iterator<String> objectIterator;

    KeyIterator() {
      this.objectIterator = JSSparseArray.super.keys().iterator();
    }

    @Override
    public final boolean hasNext() {
      return currentIndex < elements.size() || objectIterator.hasNext();
    }

    @Override
    public final String next() {
      if (currentIndex < elements.size()) {
        return String.valueOf(elements.keyAt(currentIndex++));
      }
      return objectIterator.next();
    }
  }

  private final class KeySet extends AbstractSet<String> {
    @Override
    public final Iterator<String> iterator() {
     return new KeyIterator();
    }

    @Override
    public final int size() {
      return JSSparseArray.this.fieldCount();
    }
  }
  // endregion

  // region values
  @Override
  public Collection<Object> values() {
    Collection<Object> vc;
    return (vc = valueCollection) == null ? (valueCollection = new ValueCollection()) : vc;
  }

  private final class ValueIterator implements Iterator<Object> {
    private int currentIndex = 0;
    private final Iterator<Object> objectIterator;

    ValueIterator() {
      objectIterator = JSSparseArray.super.values().iterator();
    }

    @Override
    public final boolean hasNext() {
      return currentIndex < elements.size() || (objectIterator != null && objectIterator.hasNext());
    }

    @Override
    public final Object next() {
      if (currentIndex < elements.size()) {
        return elements.valueAt(currentIndex++);
      } else if (objectIterator != null) {
        return objectIterator.next();
      }
      throw new NoSuchElementException();
    }
  }

  private final class ValueCollection extends AbstractCollection<Object> {
    @Override
    public final Iterator<Object> iterator() {
      return new ValueIterator();
    }

    @Override
    public final int size() {
      return JSSparseArray.this.fieldCount();
    }
  }
  // endregion

  // region entries
  @Override
  public Set<Pair<String, Object>> entries() {
    Set<Pair<String, Object>> es;
    return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
  }

  private final class EntryIterator implements Iterator<Pair<String, Object>> {
    private final Iterator<Pair<String, Object>> objectIterator;
    private int currentIndex = 0;

    EntryIterator() {
      this.objectIterator = JSSparseArray.super.entries().iterator();
    }

    @Override
    public boolean hasNext() {
      return currentIndex < elements.size() || objectIterator.hasNext();
    }

    @Override
    public Pair<String, Object> next() {
      if (currentIndex < size()) {
        Pair<String, Object> pair = new Pair<>(String.valueOf(elements.keyAt(currentIndex)), elements.valueAt(currentIndex));
        currentIndex++;
        return pair;
      }
      return objectIterator.next();
    }
  }

  private final class EntrySet extends AbstractSet<Pair<String, Object>> {
    @Override
    public Iterator<Pair<String, Object>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      return JSSparseArray.this.fieldCount();
    }
  }
  // endregion

  // region items
  public Set<Pair<Integer, Object>> items() {
    Set<Pair<Integer, Object>> items;
    return (items = itemSet) == null ? (itemSet = new ItemSet()) : items;
  }
  private final class ItemIterator implements Iterator<Pair<Integer, Object>> {
    private int currentIndex = 0;

    @Override
    public boolean hasNext() {
      return currentIndex < elements.size();
    }

    @Override
    public Pair<Integer, Object> next() {
      if (currentIndex < size()) {
        Pair<Integer, Object> pair = new Pair<>(elements.keyAt(currentIndex), elements.valueAt(currentIndex));
        currentIndex++;
        return pair;
      }
      throw new NoSuchElementException();
    }
  }

  private final class ItemSet extends AbstractSet<Pair<Integer, Object>> {
    @Override
    public Iterator<Pair<Integer, Object>> iterator() {
      return new ItemIterator();
    }

    @Override
    public int size() {
      return JSSparseArray.this.elements.size();
    }
  }
  // endregion

  // region json
  public static JSSparseArray load(JSONArray json) throws JSONException {
    JSSparseArray array = new JSSparseArray(json.length());
    for (int i = 0; i < json.length(); i ++) {
      array.push(JSValue.load(json.get(i)));
    }
    return array;
  }

  @Override
  public Object dump() throws JSONException {
    JSONObject json = (JSONObject) super.dump();
    for (int i = 0; i < elements.size(); i++) {
      json.put(String.valueOf(elements.keyAt(i)), JSValue.dump(elements.valueAt(i)));
    }
    return json;
  }
  // endregion

  @Override
  public JSSparseArray clone() throws CloneNotSupportedException {
    JSSparseArray clonedObject = (JSSparseArray) super.clone();
    SparseArray<Object> destElements = new SparseArray<>(elements.size());
    for (int i = 0; i < elements.size(); i++) {
      destElements.append(elements.keyAt(i), elements.valueAt(i));
    }
    clonedObject.elements = destElements;
    return clonedObject;
  }
}
