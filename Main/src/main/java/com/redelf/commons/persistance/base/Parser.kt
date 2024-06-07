package com.redelf.commons.persistance.base

import java.lang.reflect.Type

interface Parser {

    fun <T> fromJson(content: String?, type: Type?): T?

    fun toJson(body: Any?): String?
}
