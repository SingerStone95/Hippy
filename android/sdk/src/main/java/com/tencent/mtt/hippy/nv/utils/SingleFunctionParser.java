package com.tencent.mtt.hippy.nv.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SingleFunctionParser<V> extends FunctionParser<String, List<V>> {

    public interface FlatMapper<V> {

        V map(String raw);
    }

    public interface NonUniformMapper<V> {
        List<V> map(List<String> raw);
    }

    /**
     * Construct a function parser for uniform parameters.
     *
     * @param source the raw string representation of a group of function(s)
     * @param mapper the mapping rule between string and corresponding type of object.
     */
    public SingleFunctionParser(String source, final FlatMapper<V> mapper) {
        super(source, new Mapper<String, List<V>>() {
            @Override
            public Map<String, List<V>> map(String functionName, List<String> raw) {
                Map<String, List<V>> map = new HashMap<String, List<V>>();
                List<V> list = new LinkedList<V>();
                for (String item : raw) {
                    list.add(mapper.map(item));
                }
                map.put(functionName, list);
                return map;
            }
        });
    }

    /**
     * Construct a function parser for non-uniform parameters.
     *
     * @param source the raw string representation of a group of function(s)
     * @param mapper the mapping rule between string and corresponding type of object.
     */
    public SingleFunctionParser(String source, final NonUniformMapper<V> mapper) {
        super(source, new Mapper<String, List<V>>() {
            @Override
            public Map<String, List<V>> map(String functionName, List<String> raw) {
                Map<String, List<V>> map = new HashMap<String, List<V>>();
                map.put(functionName, mapper.map(raw));
                return map;
            }
        });
    }

    public List<V> parse(String functionName) {
        Map<String, List<V>> map = parse();
        if (map.containsKey(functionName)) {
            return map.get(functionName);
        }
        return null;
    }
}
