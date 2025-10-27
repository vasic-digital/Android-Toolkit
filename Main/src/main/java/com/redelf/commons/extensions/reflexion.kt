/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.extensions

import com.google.gson.annotations.Expose
import com.redelf.commons.logging.Console
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Type

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

            val field = instance::class.java.getAllFields().find { it.name == fieldName }

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

    } catch (e: Throwable) {

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

    } catch (e: Throwable) {

        recordException(e)
    }

    return excluded
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun Class<*>.getSuperClasses(selfIncluded: Boolean = true): Set<Class<*>> {

    val superClasses = mutableSetOf<Class<*>>()

    if (selfIncluded) {

        superClasses.add(this)
    }

    try {

        var currentClazz = this.superclass

        while (currentClazz != null) {

            superClasses.add(currentClazz)
            currentClazz = currentClazz.superclass
        }

    } catch (e: Throwable) {

        recordException(e)
    }

    return superClasses
}

fun Class<*>.getAllFields(): Set<Field> {

    val allFields = mutableSetOf<Field>()

    try {

        var classes = this.getSuperClasses()

        classes.forEach {

            val fields = it.declaredFields
            allFields.addAll(fields)
        }

    } catch (e: Throwable) {

        recordException(e)
    }

    return allFields
}

fun Class<*>.getFieldByName(name: String): Field? {

    try {

        val allFields = this.getAllFields()

        allFields.forEach {

            if (it.name == name) {

                return it
            }
        }

    } catch (e: Throwable) {

        recordException(e)
    }

    return null
}

fun String.toClass(): Class<*>? {

    try {

        return Class.forName(this.forClassName())

    } catch (e: Throwable) {

        recordException(e)
    }

    return null
}

fun Type.getRawCassName(): String {

    try {

        return typeName.forClassName().replaceAfter("<", "").replace("<", "")

    } catch (e: Throwable) {

        recordException(e)
    }

    return ""
}
