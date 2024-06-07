package com.redelf.commons.persistance.base

import java.lang.reflect.Type

interface Parser {

    fun toJson(body: Any?): String?

    fun <T> fromJson(content: String?, type: Type?): T?
}
