package com.codesentinel.common.databuffer;

import java.util.concurrent.ConcurrentHashMap;

public class DataBuffer {

    private static final ConcurrentHashMap<String, Object> CACHE =
            new ConcurrentHashMap<>();


    private DataBuffer() {
    }


    public static void put(String key, Object value) {
        CACHE.put(key, value);
    }


    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) CACHE.get(key);
    }


    public static void remove(String key) {
        CACHE.remove(key);
    }


    public static void clear() {
        CACHE.clear();
    }
}
