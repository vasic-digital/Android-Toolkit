package com.redelf.commons.persistance;

import com.google.gson.reflect.TypeToken;
import com.redelf.commons.obtain.Obtain;
import com.redelf.commons.persistance.base.Converter;
import com.redelf.commons.persistance.base.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete implementation of encoding and decoding.
 * List types will be encoded/decoded by parser
 * Serializable types will be encoded/decoded object stream
 * Not serializable objects will be encoded/decoded by parser
 */
final class DataConverter implements Converter {

    private final Obtain<Parser> parser;

    public DataConverter(Obtain<Parser> parser) {
        if (parser == null) {
            throw new NullPointerException("Parser should not be null");
        }
        this.parser = parser;
    }

    @Override
    public <T> String toString(T value) {
        if (value == null) {
            return null;
        }
        final Parser p = parser.obtain();
        return p.toJson(value);
    }

    @Override
    public <T> T fromString(String value, DataInfo info) throws Exception {

        if (value == null) {

            return null;
        }

        if (info == null) {

            return null;
        }

        Class<?> keyType = info.getKeyClazz();
        Class<?> valueType = info.getValueClazz();

        switch (info.getDataType()) {
            case DataInfo.TYPE_OBJECT:
                return toObject(value, keyType);
            case DataInfo.TYPE_LIST:
                return toList(value, keyType);
            case DataInfo.TYPE_MAP:
                return toMap(value, keyType, valueType);
            case DataInfo.TYPE_SET:
                return toSet(value, keyType);
            default:
                return null;
        }
    }

    private <T> T toObject(String json, Class<?> type) throws Exception {
        final Parser p = parser.obtain();
        return p.fromJson(json, type);
    }

    @SuppressWarnings("unchecked")
    private <T> T toList(String json, Class<?> type) throws Exception {
        if (type == null) {
            return (T) new ArrayList<>();
        }
        final Parser p = parser.obtain();
        List<T> list = p.fromJson(
                json,
                new TypeToken<List<T>>() {
                }.getType()
        );

        int size = list.size();
        for (int i = 0; i < size; i++) {
            list.set(i, (T) p.fromJson(p.toJson(list.get(i)), type));
        }
        return (T) list;
    }

    @SuppressWarnings("unchecked")
    private <T> T toSet(String json, Class<?> type) throws Exception {
        Set<T> resultSet = new HashSet<>();
        if (type == null) {
            return (T) resultSet;
        }
        final Parser p = parser.obtain();
        Set<T> set = p.fromJson(json, new TypeToken<Set<T>>() {
        }.getType());

        for (T t : set) {
            String valueJson = p.toJson(t);
            T value = p.fromJson(valueJson, type);
            resultSet.add(value);
        }
        return (T) resultSet;
    }

    @SuppressWarnings("unchecked")
    private <K, V, T> T toMap(String json, Class<?> keyType, Class<?> valueType) throws Exception {
        Map<K, V> resultMap = new HashMap<>();
        if (keyType == null || valueType == null) {
            return (T) resultMap;
        }
        final Parser p = parser.obtain();
        Map<K, V> map = p.fromJson(json, new TypeToken<Map<K, V>>() {
        }.getType());

        for (Map.Entry<K, V> entry : map.entrySet()) {
            String keyJson = p.toJson(entry.getKey());
            K k = p.fromJson(keyJson, keyType);

            String valueJson = p.toJson(entry.getValue());
            V v = p.fromJson(valueJson, valueType);
            resultMap.put(k, v);
        }
        return (T) resultMap;
    }
}
