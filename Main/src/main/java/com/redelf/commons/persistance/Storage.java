package com.redelf.commons.persistance;

import com.redelf.commons.lifecycle.InitializationWithContext;
import com.redelf.commons.lifecycle.TerminationSynchronized;

public interface Storage<T> extends TerminationSynchronized, InitializationWithContext {

  boolean put(String key, T value);

  T get(String key);

  boolean delete(String key);

  boolean deleteAll();

  long count();

  boolean contains(String key);
}
