package com.redelf.commons.data;

import com.redelf.commons.management.DataManagement;

public class Storage {

    public static <T> boolean put(String key, T value) {

        return DataManagement.STORAGE.push(key, value);
    }

    public static <T> T get(String key) {

        return DataManagement.STORAGE.pull(key);
    }

    public static <T> T get(String key, T defaultValue) {

        final T res =  DataManagement.STORAGE.pull(key) ;
        return res == null ? defaultValue : res;
    }

    public static boolean delete(String key) {

        return DataManagement.STORAGE.delete(key);
    }

    public static boolean deleteAll() {

        return DataManagement.STORAGE.erase();
    }

    public static boolean contains(String key) {

        return DataManagement.STORAGE.contains(key);
    }
}
