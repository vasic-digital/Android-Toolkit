package com.redelf.commons.extensions

import com.redelf.commons.logging.Console
import java.lang.reflect.Modifier

fun Class<*>.hasPublicDefaultConstructor(): Boolean {

    return try {

        val constructor = this.getConstructor()
        Modifier.isPublic(constructor.modifiers)

    } catch (_: NoSuchMethodException) {

        false
    }
}

fun Any.assign(fieldName: String, fieldValue: Any?): Boolean {

    val tag = "ASSIGN :: Instance = '$this' :: Field = '$fieldName' " +
            ":: Value = '$fieldValue' ::"

    Console.log("$tag START")

    try {

        this.let { instance ->

            val field = instance::class.java.declaredFields.find { it.name == fieldName }

            field?.let {

                it.isAccessible = true
                it.set(instance, fieldValue)

                val success = it.get(instance) == fieldValue

                if (success) {

                    Console.log("$tag END")

                } else {

                    Console.error("$tag FAILED")
                }

                return success
            }
        }

    } catch (e: Exception) {

        Console.error("$tag ERROR: ${e.message}")
        recordException(e)
    }

    return false
}