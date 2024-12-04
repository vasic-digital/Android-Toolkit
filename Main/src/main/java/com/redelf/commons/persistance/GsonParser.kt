package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.ByteArraySerializer
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer
import com.redelf.commons.persistance.serialization.Serializer
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

/*
* TODO: Test for this is mandatory
*/
class GsonParser(

    parserKey: String,
    private val provider: Obtain<GsonBuilder>

) : Parser {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val gson = provider.obtain().create()
    private val ctx: Context = BaseApplication.takeContext()
    private val tag = "Parser :: GSON :: Key = '$parserKey', Hash = '${hashCode()}'"
    private val byteArraySerializer = ByteArraySerializer(ctx, "Parser.GSON.$parserKey")

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

    /*
    * TODO: Catch and handle exceptions
    */
    private fun createTypeAdapter(

        who: Any,
        recipe: Map<String, Serializer>

    ): TypeAdapter<Any> {

        val clazz = who.javaClass
        val tag = "$tag Type adapter :: Class = '${clazz.canonicalName}'"

        Console.log("$tag CREATE :: Recipe = $recipe")

        return object : TypeAdapter<Any>() {

            override fun write(out: JsonWriter?, value: Any?) {

                if (value == null) {

                    out?.nullValue()

                } else {

                    out?.beginObject()

                    val fields = clazz.declaredFields

                    fields.forEach { field ->

                        var excluded = false
                        field.isAccessible = true

                        if (field.isAnnotationPresent(Expose::class.java)) {

                            val exposeAnnotation = field.getAnnotation(Expose::class.java)

                            if (exposeAnnotation?.serialize == true) {

                                val value = field.get(who)

                                if (value is Boolean) {

                                    excluded = value
                                }
                            }
                        }

                        if (!excluded) {

                            excluded = field.isAnnotationPresent(Transient::class.java)
                        }

                        val fieldName = field.name

                        if (excluded) {

                            Console.log("$tag EXCLUDED :: Field name = '$fieldName'")

                        } else {

                            val wTag = "$tag WRITING :: Field name = '$fieldName' ::"

                            Console.log("$wTag START")

                            val fieldValue = field.get(who)

                            fieldValue?.let { fValue ->

                                fun regularWrite() {

                                    val rwTag = "$wTag REGULAR WRITE ::"

                                    Console.log("$rwTag START")

                                    out?.name(fieldName)
                                    out?.value(gson.toJson(fValue))

                                    Console.log("$rwTag END")
                                }

                                fun customWrite() {

                                    Console.log(

                                        "$wTag Custom write :: START :: " +
                                                "Class = '${clazz.canonicalName}'"
                                    )

                                    recipe[fieldName]?.let { serializer ->

                                        if (serializer is DefaultCustomSerializer) {

                                            Console.log("$wTag Custom write :: Custom serializer")

                                            when (clazz.canonicalName) {

                                                ByteArray::class.java.canonicalName -> {

                                                    out?.name(fieldName)
                                                    out?.value(byteArraySerializer.serialize(fieldName, fValue))
                                                }
                                            }

                                        } else {

                                            Console.log(

                                                "$wTag Custom write :: Custom provided serializer"
                                            )

                                            out?.name(fieldName)
                                            out?.value(serializer.serialize(fieldName, fValue))
                                        }
                                    }

                                    if (recipe[fieldName] == null) {

                                        Console.log("$wTag END :: To write regular (1)")

                                        regularWrite()
                                    }
                                }

                                if (recipe.containsKey(fieldName)) {

                                    Console.log("$wTag END :: To write custom")

                                    customWrite()

                                } else {

                                    Console.log("$wTag END :: To write regular (2)")

                                    regularWrite()
                                }
                            }

                            if (fieldValue == null) {

                                Console.log("$wTag END :: Field value is null")
                            }
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
