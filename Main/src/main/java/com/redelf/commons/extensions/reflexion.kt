package com.redelf.commons.extensions

import com.google.gson.annotations.Expose
import com.redelf.commons.logging.Console
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun Class<*>.hasPublicDefaultConstructor(): Boolean {

    return try {

        val constructor = this.getConstructor()
        Modifier.isPublic(constructor.modifiers)

    } catch (_: NoSuchMethodException) {

        false
    }
}

fun Any.assign(fieldName: String, fieldValue: Any?, tag: String = ""): Boolean {

    val tag = "$tag ASSIGN :: Instance = '$this' :: Field = '$fieldName' " +
            ":: Value = '$fieldValue' ::".trim()

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

fun Field.isExcluded(instance: Any): Boolean {

    var excluded = false

    try {

        this.isAccessible = true

        if (this.isAnnotationPresent(Expose::class.java)) {

            val exposeAnnotation = this.getAnnotation(Expose::class.java)

            if (exposeAnnotation?.serialize == true) {

                val value = this.get(instance)

                if (value is Boolean) {

                    excluded = value
                }
            }
        }

        if (!excluded) {

            excluded = this.isAnnotationPresent(Transient::class.java)
        }

    } catch (e: Exception) {

        recordException(e)
    }

    return excluded
}