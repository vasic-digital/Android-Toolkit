package com.redelf.commons.persistance;

import com.redelf.commons.lifecycle.TerminationSynchronized;

/**
 * Intermediate layer which stores the given data. Used by Data.
 *
 * <p>Use custom implementation if the built-in implementations are not enough.</p>
 *
 * @see SharedPreferencesStorage
 */
public interface Storage<T> extends TerminationSynchronized {

  boolean put(String key, T value);

  T get(String key);

  /**
   * Remove single entry from storage
   *
   * @param key the name of entry to delete
   *
   * @return true if removal is successful, otherwise false
   */
  boolean delete(String key);

  boolean deleteAll();

  long count();

  boolean contains(String key);
}
