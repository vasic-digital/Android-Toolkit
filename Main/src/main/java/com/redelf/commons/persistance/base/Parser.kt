package com.redelf.commons.persistance.base

import java.lang.reflect.Type

interface Parser {

    fun fromJson(content: String?, type: Type?): Any?

    fun fromJson(content: String?, clazz: Class<*>?): Any?

    fun toJson(body: Any?): String?
}
