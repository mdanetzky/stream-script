package com.mdanetzky.streamscript.parser.js;

import io.vavr.collection.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

class GlobalVavrMap extends SimpleBindings implements Closeable {

    private final static String NASHORN_GLOBAL = "nashorn.global";
    private Bindings global;
    private Set<String> original_keys;
    private Map<String, Object> map;

    GlobalVavrMap(Map<String, Object> map) {
        super(map.toJavaMap());
        this.map = map;
    }

    @Override
    public Object put(String key, Object value) {
        if (key.equals(NASHORN_GLOBAL) && value instanceof Bindings) {
            global = (Bindings) value;
            original_keys = new HashSet<>(global.keySet());
            return null;
        }
        map = map.put(key, value);
        return super.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return key.equals(NASHORN_GLOBAL) ? global : super.get(key);
    }

    public Map<String, Object> getMap() {
        return this.map;
    }

    @Override
    public void close() {
        if (global != null) {
            Set<String> keys = new HashSet<>(global.keySet());
            keys.removeAll(original_keys);
            for (String key : keys)
                put(key, global.remove(key));
        }
    }
}
