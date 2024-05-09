package com.redelf.commons.persistance;

import com.redelf.commons.lifecycle.InitializationWithContext;
import com.redelf.commons.lifecycle.TerminationSynchronized;

public interface Storage<T> extends Put<Boolean>, Get<Boolean>,TerminationSynchronized, InitializationWithContext {

  boolean delete(String key);

  boolean deleteAll();

  long count();

  boolean contains(String key);
}
