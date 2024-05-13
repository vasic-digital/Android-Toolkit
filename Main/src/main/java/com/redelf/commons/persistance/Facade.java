package com.redelf.commons.persistance;

import android.content.Context;

import androidx.annotation.NonNull;

import com.redelf.commons.lifecycle.InitializationWithContext;
import com.redelf.commons.lifecycle.ShutdownSynchronized;
import com.redelf.commons.lifecycle.TerminationSynchronized;

public interface Facade extends

        ShutdownSynchronized,
        TerminationSynchronized,
        InitializationWithContext

{

  <T> boolean put(String key, T value);

  <T> T get(String key);

  <T> T get(String key, T defaultValue);

  long count();

  boolean deleteAll();

  boolean delete(String key);


  boolean contains(String key);

  boolean isBuilt();

  void destroy();

  class EmptyFacade implements Facade {

    @Override public <T> boolean put(String key, T value) {
      throwValidation();
      return false;
    }

    @Override public <T> T get(String key) {
      throwValidation();
      return null;
    }

    @Override public <T> T get(String key, T defaultValue) {
      throwValidation();
      return null;
    }

    @Override public long count() {
      throwValidation();
      return 0;
    }

    @Override public boolean deleteAll() {
      throwValidation();
      return false;
    }

    @Override public boolean delete(String key) {
      throwValidation();
      return false;
    }

    @Override public boolean contains(String key) {
      throwValidation();
      return false;
    }

    @Override public boolean isBuilt() {
      return false;
    }

    @Override public void destroy() {
      throwValidation();
    }

    @Override
    public boolean shutdown() {
      return true;
    }

    @Override
    public boolean terminate() {
      return true;
    }

    @Override
    public void initialize(@NonNull Context ctx) {

      // Ignore
    }

    private void throwValidation() {
      throw new IllegalStateException("Data is not built. " +
          "Please call build() and wait the initialisation finishes.");
    }
  }
}
