package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.assign
import com.redelf.commons.extensions.fromBase64
import com.redelf.commons.extensions.hasPublicDefaultConstructor
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isExcluded
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.toBase64
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.ByteArraySerializer
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer
import com.redelf.commons.persistance.serialization.Serializer
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

class GsonParser(

    parserKey: String,
    private val provider: Obtain<GsonBuilder>

) : Parser {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val ctx: Context = BaseApplication.takeContext()
    private val tag = "Parser :: GSON :: Key = '$parserKey', Hash = '${hashCode()}'"
    private val byteArraySerializer = ByteArraySerializer(ctx, "Parser.GSON.$parserKey")

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, type: Type?): T? {

        try {

            if (isEmpty(content)) {

                return null
            }

            if (type == null) {

                return null
            }

            val tag = "$tag Deserialize :: Type = '${type.typeName}' ::"

            Console.log("$tag START")

            type.let { t ->

                try {

                    val clazz = Class.forName(t.typeName)

                    Console.log("$tag Class = '${clazz.canonicalName}'")

                    val instance: Any? = fromJson(content, clazz)

                    Console.log("$tag END :: Instance = '$instance'")

                    return instance as T?

                } catch (e: Exception) {

                    Console.error("$tag ERROR: ${e.message}")
                    recordException(e)
                }
            }

            // FIXME: Use type adapter
            return gson.fromJson(content, type)

        } catch (e: Exception) {

            recordException(e)
            Console.error("$tag ERROR: ${e.message}")
        }

        return null
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, clazz: Class<*>?): T? {

        if (isEmpty(content)) {

            return null
        }

        var typeAdapter: TypeAdapter<*>? = null
        val tag = "$tag Deserialize :: Class = '${clazz?.canonicalName}'"

        Console.log("$tag START")

        try {

            Console.log("$tag Class = '${clazz?.canonicalName}'")

            val instance = instantiate(clazz)

            Console.log("$tag Instance hash = ${instance.hashCode()}")

            if (instance is CustomSerializable) {

                val customizations = instance.getCustomSerializations()

                Console.log("$tag Customizations = $customizations")

                typeAdapter = createTypeAdapter(instance, customizations)

                Console.log("$tag Type adapter registered")
            }

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }

        if (typeAdapter == null) {

            Console.error("$tag ERROR: Type adapter is null")
        }

        try {

            typeAdapter?.let { adapter ->

                return adapter.fromJson(content) as T?
            }


        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}, Content = '$content'")
            recordException(e)
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

            var typeAdapter: TypeAdapter<*>? = null

            if (body is CustomSerializable) {

                val customizations = body.getCustomSerializations()

                Console.log("$tag Customizations = $customizations")

                typeAdapter = createTypeAdapter(body, customizations)

                Console.log("$tag Type adapter registered")
            }

            typeAdapter?.let { adapter ->

                return adapter.toJson(body)
            }

            // FIXME: Use type adapter
            return gson.toJson(body)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }

    private fun createTypeAdapter(

        instance: Any,
        recipe: Map<String, Serializer>

    ): TypeAdapter<Any> {

        val clazz = instance::class.java
        val gson = provider.obtain().create()
        val tag = "$tag Type adapter :: Class = '${clazz.canonicalName}'"

        Console.log("$tag CREATE :: Recipe = $recipe")

        return object : TypeAdapter<Any>() {

            override fun write(out: JsonWriter?, value: Any?) {

                try {

                    if (value == null) {

                        out?.nullValue()

                    } else {

                        out?.beginObject()

                        val fields = clazz.declaredFields

                        fields.forEach { field ->

                            val fieldName = field.name
                            val excluded = field.isExcluded(instance)

                            if (excluded) {

                                Console.log("$tag EXCLUDED :: Field name = '$fieldName'")

                            } else {

                                val wTag = "$tag WRITING :: Field name = '$fieldName' ::"

                                Console.log("$wTag START")

                                val fieldValue = field.get(instance)

                                fieldValue?.let { fValue ->

                                    val value = fValue
                                    val clazz = fValue::class.java

                                    fun regularWrite() {

                                        val rwTag = "$wTag REGULAR WRITE ::"

                                        Console.log("$rwTag START")

                                        try {

                                            val serialized = gson.toJson(value).toBase64()

                                            out?.name(fieldName)
                                            out?.value(serialized)

                                            Console.log("$rwTag END")

                                        } catch (e: Exception) {

                                            Console.error("$rwTag ERROR: ${e.message}")
                                            recordException(e)
                                        }
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

                                                        try {

                                                            byteArraySerializer.serialize(

                                                                fieldName,
                                                                fValue
                                                            )

                                                        } catch (e: Exception) {

                                                            Console.error("$wTag ERROR: ${e.message}")
                                                            recordException(e)
                                                        }
                                                    }

                                                    else -> {

                                                        val e = IllegalArgumentException(

                                                            "Not supported type for default " +
                                                                    "custom serializer " +
                                                                    "'${clazz.canonicalName}'"
                                                        )

                                                        Console.error("$wTag ERROR: ${e.message}")
                                                        recordException(e)
                                                    }
                                                }

                                            } else {

                                                Console.log(

                                                    "$wTag Custom write :: Custom provided serializer"
                                                )

                                                try {

                                                    serializer.serialize(

                                                        fieldName,
                                                        fValue
                                                    )

                                                } catch (e: Exception) {

                                                    Console.error("$tag ERROR: ${e.message}")
                                                    recordException(e)
                                                }
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

                } catch (e: Exception) {

                    recordException(e)
                }
            }

            @Suppress("DEPRECATION")
            override fun read(`in`: JsonReader?): Any? {

                val tag = "$tag READ ::"

                Console.log("$tag START")

                try {

                    `in`?.beginObject()

                    val fieldsRead = mutableListOf<String>()

                    while (`in`?.hasNext() == true) {

                        val fieldName = `in`.nextName()

                        val tag = "$tag Field = '$fieldName' ::"

                        fieldsRead.add(fieldName)

                        fun customRead(): Any? {

                            val tag = "$tag CUSTOM ::"

                            Console.log("$tag START")

                            try {

                                recipe[fieldName]?.let { serializer ->

                                    if (serializer is DefaultCustomSerializer) {

                                        val clazz = serializer.takeClass()

                                        Console.log(

                                            "$tag Custom write :: Custom serializer :: " +
                                                    "Class = '${clazz.canonicalName}'"
                                        )

                                        when (clazz.canonicalName) {

                                            ByteArray::class.java.canonicalName -> {

                                                val result = byteArraySerializer.deserialize(fieldName)

                                                return result
                                            }

                                            else -> {

                                                val e = IllegalArgumentException(

                                                    "Not supported type for default " +
                                                            "custom serializer " +
                                                            "'${clazz.canonicalName}'"
                                                )

                                                Console.error("$tag ERROR: ${e.message}")
                                                recordException(e)

                                                return null
                                            }
                                        }

                                    } else {

                                        val result = serializer.deserialize(fieldName)

                                        return result
                                    }
                                }

                            } catch (e: Exception) {

                                Console.error("$tag ERROR: ${e.message}")
                                recordException(e)
                            }

                            return null
                        }

                        fun regularRead(): Any? {

                            val tag = "$tag REGULAR ::"

                            Console.log("$tag START")

                            try {

                                val json = `in`.nextString()

                                Console.log("$tag JSON obtained")

                                val result = gson.fromJson(json.fromBase64(), clazz)

                                Console.log("$tag END: $result")

                                return ""

                            } catch (e: Exception) {

                                Console.error("$tag ERROR: ${e.message}")
                                recordException(e)
                            }

                            return null
                        }

                        if (recipe.containsKey(fieldName)) {

                            val read = customRead()

                            Console.log("$tag Read = '$read'")

                            assign(instance, fieldName, read)

                            Console.log("$tag Assigned = '$read'")

                        } else {

                            val read = regularRead()

                            Console.log("$tag Read = '$read'")

                            assign(instance, fieldName, read)

                            Console.log("$tag Assigned = '$read'")
                        }
                    }

                    val fields = clazz.declaredFields

                    fields.forEach { field ->

                        val fieldName = field.name

                        val tag = "$tag ADDITIONAL :: Field = '$fieldName' ::"

                        if (!fieldsRead.contains(fieldName) && !field.isExcluded(instance)) {

                            Console.log("$tag START")

                            // TODO: Do deserialize

                            Console.log("$tag END")
                        }
                    }

                    `in`?.endObject()

                    return instance

                } catch (e: Exception) {

                    Console.error("$tag ERROR: ${e.message}")
                    recordException(e)
                }

                return null
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun instantiate(clazz: Class<*>?): Any? {

        if (clazz == null) {

            return null
        }

        Console.log("$tag INSTANTIATE :: Class = '${clazz.canonicalName}' :: START")

        try {

            var instance: Any? = null

            if (clazz.hasPublicDefaultConstructor()) {

                instance = clazz.newInstance()

                Console.log("$tag INSTANTIATE :: END :: Instance = '$instance'")

            } else {

                Console.error("$tag INSTANTIATE :: END :: No public constructor found")
            }


            return instance

        } catch (e: Exception) {

            Console.error("$tag INSTANTIATE :: ERROR: ${e.message}")
            recordException(e)
        }

        return null
    }

    private fun assign(instance: Any?, fieldName: String, fieldValue: Any?) {

        val tag = "$tag ASSIGN :: Instance = '$instance' :: Field = '$fieldName' " +
                ":: Value = '$fieldValue' ::"

        instance?.assign(fieldName, fieldValue, tag)
    }
}
