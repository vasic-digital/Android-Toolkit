package com.redelf.commons.persistance;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

final class SharedPreferencesStorage implements Storage<String> {

    private final SharedPreferences preferences;

    SharedPreferencesStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean shutdown() {

        return true;
    }

    @Override
    public void initialize(@NonNull Context ctx) {

        // Ignore
    }

    @Override
    public boolean put(String key, String value) {

        PersistenceUtils.checkNull("key", key);
        return getEditor().putString(key, String.valueOf(value)).commit();
    }

    @Override
    public String get(String key) {
        return preferences.getString(key, "");
    }

    @Override
    public boolean delete(String key) {
        return getEditor().remove(key).commit();
    }

    @Override
    public boolean contains(String key) {
        return preferences.contains(key);
    }

    @Override
    public boolean deleteAll() {
        return getEditor().clear().commit();
    }

    @Override
    public long count() {
        return preferences.getAll().size();
    }

    private SharedPreferences.Editor getEditor() {
        return preferences.edit();
    }

}
