package com.redelf.commons.persistance

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.CustomSerializable
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

class GsonParser(private val provider: Obtain<GsonBuilder>) : Parser {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "Parser :: GSON ::"

    override fun <T> fromJson(content: String?, type: Type?): T? {

        try {

            if (isEmpty(content)) {

                return null
            }

            val gsonProvider = provider.obtain()

//            if (body is CustomSerializable) {
//
//                val customizations = body.getCustomSerializations()
//
//                Console.log(
//
//                    "$tag Custom serialization :: " +
//                            "Class = ${body::class.java.canonicalName}, " +
//                            "Customizations = $customizations"
//                )

            // TODO: Apply customizations
//            }

            return gsonProvider.create().fromJson(content, type)

        } catch (e: Exception) {

            recordException(e)

            Console.error("Tried to deserialize into '${type?.typeName}' from '$content'")
        }

        return null
    }

    override fun <T> fromJson(content: String?, clazz: Class<T>?): T? {

        try {

            if (isEmpty(content)) {

                return null
            }

            val gsonProvider = provider.obtain()

//            if (body is CustomSerializable) {
//
//                val customizations = body.getCustomSerializations()
//
//                Console.log(
//
//                    "$tag Custom serialization :: " +
//                            "Class = ${body::class.java.canonicalName}, " +
//                            "Customizations = $customizations"
//                )

                // TODO: Apply customizations
//            }

            return gsonProvider.create().fromJson(content, clazz)

        } catch (e: Exception) {

            recordException(e)

            Console.error("Tried to deserialize into '${clazz?.simpleName}' from '$content'")
        }

        return null
    }

    override fun toJson(body: Any?): String? {

        if (body == null) {

            return null
        }

        val tag = "$tag Class = '${body::class.java.canonicalName}' ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            val gsonProvider = provider.obtain()

            if (body is CustomSerializable) {

                val customizations = body.getCustomSerializations()

                Console.log("$tag Customizations = $customizations")

                val typeAdapter = createTypeAdapter(body::class.java, customizations)

                gsonProvider.registerTypeAdapter(body::class.java, typeAdapter)

                Console.log("$tag Type adapter registered")
            }

            return gsonProvider.create().toJson(body)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }

    private fun createTypeAdapter(who: Any, recipe: Map<String, String>): TypeAdapter<Any> {

        val clazz = who.javaClass
        val tag = "$tag Type adapter :: Create :: Class = '${clazz.canonicalName}'"

        Console.log("$tag START :: Recipe = $recipe")

        return object : TypeAdapter<Any>() {

            override fun write(out: JsonWriter?, value: Any?) {

                if (value == null) {

                    out?.nullValue()

                } else {

                    out?.beginObject()

                    val fields = clazz.declaredFields

                    fields.forEach { field ->

                        field.isAccessible = true

                        if (field.isAnnotationPresent(Transient::class.java)) {

                            // TODO: Implement skipping

                        } else {

                            val fieldName = field.name
                            val fieldValue = field.get(who)
                        }
                    }

                    out?.endObject()
                }
            }

            override fun read(`in`: JsonReader?): Any? {

                TODO("Not yet implemented")
            }
        }
    }
}
