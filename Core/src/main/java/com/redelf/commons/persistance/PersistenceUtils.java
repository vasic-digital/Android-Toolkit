package com.redelf.commons.persistance;

final class PersistenceUtils {

  private PersistenceUtils() {
    //no instance
  }

  public static void checkNull(String message, Object value) {
    if (value == null) {
      throw new NullPointerException(message + " should not be null");
    }
  }

  public static void checkNullOrEmpty(String message, String value) {
    if (isEmpty(value)) {
      throw new NullPointerException(message + " should not be null or empty");
    }
  }

  public static void checkNullOrEmpty(String message, byte[] value) {
    if (isEmpty(value)) {
      throw new NullPointerException(message + " should not be null or empty");
    }
  }

  public static boolean isEmpty(String text) {
    return text == null || text.trim().length() == 0;
  }

  public static boolean isEmpty(byte[] text) {
    return text == null || text.length == 0;
  }
}
