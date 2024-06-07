package com.redelf.commons.persistance;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.redelf.commons.obtain.Obtain;
import com.redelf.commons.persistance.base.Parser;

import java.lang.reflect.Type;

public final class GsonParser implements Parser {

  private final Obtain<Gson> gson;

  public GsonParser(Obtain<Gson> gson) {
    this.gson = gson;
  }

  @Override public <T> T fromJson(String content, Type type) throws JsonSyntaxException {
    if (TextUtils.isEmpty(content)) {
      return null;
    }
    return gson.obtain().fromJson(content, type);
  }

  @Override public String toJson(Object body) {

    return gson.obtain().toJson(body);
  }
}
