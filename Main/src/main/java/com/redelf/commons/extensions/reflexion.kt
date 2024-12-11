package com.redelf.commons.extensions

import java.lang.reflect.Modifier

fun Class<*>.hasPublicDefaultConstructor(): Boolean {

    return try {

        val constructor = this.getConstructor()
        Modifier.isPublic(constructor.modifiers)

    } catch (_: NoSuchMethodException) {

        false
    }
}