package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.creation.instantiation.Instantiable
import com.redelf.commons.extensions.assign
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.getAllFields
import com.redelf.commons.extensions.getFieldByName
import com.redelf.commons.extensions.hasPublicDefaultConstructor
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isExcluded
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.ByteArraySerializer
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer
import com.redelf.commons.persistance.serialization.Serializer
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class GsonParser private constructor(

    encrypt: Boolean,
    encryption: Encryption<String>?,

    parserKey: String,
    provider: Obtain<GsonBuilder>

) : Parser {

    companion object : Instantiable<GsonParser> {

        val DEBUG = AtomicBoolean()

        private val instances = ConcurrentHashMap<String, GsonParser>()

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        override fun instantiate(vararg params: Any): GsonParser {

            if (params.size < 4) {

                throw IllegalArgumentException("Encryption parameters, key and provider expected")
            }

            try {

                val key = params[2] as String? ?: ""
                val encrypt = params[0] as Boolean? == true
                val encryption = params[1] as Encryption<String>?
                val provider: Obtain<GsonBuilder>? = params[2] as Obtain<GsonBuilder>?

                return instantiate(key, encryption, encrypt, provider)

            } catch (e: IllegalArgumentException) {

                throw e

            } catch (e: Exception) {

                recordException(e)

                throw IllegalStateException("ERROR: ${e.message}")
            }
        }

        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        fun instantiate(

            key: String,
            encryption: Encryption<String>?,
            encrypt: Boolean,
            provider: Obtain<GsonBuilder>?

        ): GsonParser {

            if (provider == null) {

                throw IllegalArgumentException("Provider parameter is mandatory")
            }

            var mapKey = "$key.${provider.hashCode()}.$encrypt"

            encryption?.let {

                mapKey += ".${encryption::class.simpleName}"
            }

            instances.get(mapKey)?.let {

                return it
            }

            val parser = GsonParser(encrypt, encryption, key, provider)
            instances[mapKey] = parser
            return parser
        }
    }

    private val gson = provider.obtain().create()
    private val ctx: Context = BaseApplication.takeContext()
    private val tag = "Parser :: GSON :: Key = '$parserKey', Hash = '${hashCode()}' ::"

    private val byteArraySerializer = ByteArraySerializer(

        ctx,
        "Parser.GSON.$parserKey",
        encrypt || encryption != null,
        encryption
    )

    override fun toJson(body: Any?): String? {

        if (body == null) {

            return null
        }

        val tag = "$tag Class = '${body::class.java.canonicalName?.forClassName()}' ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            var typeAdapter: TypeAdapter<Any>? = null

            if (body is CustomSerializable) {

                val customizations = body.getCustomSerializations()

                Console.log("$tag Customizations = $customizations")

                typeAdapter = createTypeAdapter(body, customizations)

                Console.log("$tag Type adapter registered")
            }

            typeAdapter?.let { adapter ->

                return adapter.toJson(body)
            }

            return gson.toJson(body)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, type: Type?): T? {

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

                val clazz = Class.forName(t.typeName.forClassName())

                Console.log("$tag Class = '${clazz.canonicalName?.forClassName()}'")

                val instance: Any? = fromJson(content, clazz)

                Console.log("$tag END :: Instance = '$instance'")

                return instance as T?

            } catch (e: Exception) {

                Console.error("$tag ERROR: ${e.message}")
                recordException(e)
            }
        }

        return null
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, clazz: Class<*>?): T? {

        if (isEmpty(content)) {

            return null
        }

        if (clazz == null) {

            return null
        }

        try {

            when (clazz.canonicalName?.forClassName()) {

                Int::class.java.canonicalName?.forClassName() -> return content?.toInt() as T?
                "int" -> return content?.toInt() as T?
                "java.lang.Integer" -> return content?.toInt() as T?
                Long::class.java.canonicalName?.forClassName() -> return content?.toLong() as T?
                "long" -> return content?.toLong() as T?
                "java.lang.Long" -> return content?.toLong() as T?
                String::class.java.canonicalName?.forClassName() -> return content as T?
                "string" -> return content as T?
                "java.lang.String" -> return content as T?
                Double::class.java.canonicalName?.forClassName() -> return content?.toDouble() as T?
                "double" -> return content?.toDouble() as T?
                "java.lang.Double" -> return content?.toDouble() as T?
                Float::class.java.canonicalName?.forClassName() -> return content?.toFloat() as T?
                "float" -> return content?.toFloat() as T?
                "java.lang.Float" -> return content?.toFloat() as T?
                Boolean::class.java.canonicalName?.forClassName() -> return content?.toBoolean() as T?
                "boolean" -> return content?.toBoolean() as T?
                "java.lang.Boolean" -> return content?.toBoolean() as T?

                else -> {

                    var typeAdapter: TypeAdapter<*>? = null

                    val tag = "$tag Deserialize :: " +
                            "Class = '${clazz.canonicalName?.forClassName()}' ::"

                    Console.log("$tag START")

                    try {

                        Console.log("$tag Class = '${clazz.canonicalName?.forClassName()}'")

                        val instance = instantiate(clazz)

                        Console.log("$tag Instance hash = ${instance.hashCode()}")

                        if (instance is CustomSerializable) {

                            val customizations = instance.getCustomSerializations()

                            Console.log("$tag Customizations = $customizations")

                            typeAdapter = createTypeAdapter(instance, customizations)

                            Console.log("$tag Type adapter registered")
                        }

                        typeAdapter?.let { adapter ->

                            return adapter.fromJson(content) as T?
                        }

                        return gson.fromJson(content, clazz) as T?

                    } catch (e: Exception) {

                        Console.error(

                            "$tag ERROR / 1 :: Content = $content, Error = '${e.message}'")

                        recordException(e)
                    }
                }
            }

        } catch (e: Exception) {

            Console.error(

                "$tag ERROR / 2 :: Class = ${clazz.canonicalName?.forClassName()}, " +
                        "Content = $content, Error = '${e.message}'"
            )

            recordException(e)
        }

        return null
    }

    private fun createTypeAdapter(

        instance: Any?,
        recipe: Map<String, Serializer> = emptyMap<String, Serializer>()

    ): TypeAdapter<Any>? {

        if (instance == null) {

            return null
        }

        val clazz = instance::class.java
        val tag = "$tag Type adapter :: Class = '${clazz.canonicalName?.forClassName()}'"

        Console.log("$tag CREATE :: Recipe = $recipe")

        return object : TypeAdapter<Any>() {

            override fun write(out: JsonWriter?, value: Any?) {

                try {

                    if (value == null) {

                        out?.nullValue()

                    } else {

                        out?.beginObject()

                        val fields = clazz.getAllFields()

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
                                    val fieldCanonical = clazz.canonicalName?.forClassName()

                                    fun regularWrite() {

                                        val rwTag = "$wTag REGULAR WRITE ::"

                                        Console.log("$rwTag START")

                                        try {

                                            out?.name(fieldName)

                                            when (fieldCanonical) {

                                                Int::class.java.canonicalName?.forClassName() -> out?.value(fValue as Int)
                                                "int" -> out?.value(fValue as Int)
                                                "java.lang.Integer" -> out?.value(fValue as Int)
                                                Long::class.java.canonicalName?.forClassName() -> out?.value(fValue as Long)
                                                "long" -> out?.value(fValue as Long)
                                                "java.lang.Long" -> out?.value(fValue as Long)
                                                String::class.java.canonicalName?.forClassName() -> out?.value(fValue as String)
                                                "string" -> out?.value(fValue as String)
                                                "java.lang.String" -> out?.value(fValue as String)
                                                Double::class.java.canonicalName?.forClassName() -> out?.value(fValue as Double)
                                                "double" -> out?.value(fValue as Double)
                                                "java.lang.Double" -> out?.value(fValue as Double)
                                                Float::class.java.canonicalName?.forClassName() -> out?.value(fValue as Float)
                                                "float" -> out?.value(fValue as Float)
                                                "java.lang.Float" -> out?.value(fValue as Float)
                                                Boolean::class.java.canonicalName?.forClassName() -> out?.value(fValue as Boolean)
                                                "boolean" -> out?.value(fValue as Boolean)
                                                "java.lang.Boolean" -> out?.value(fValue as Boolean)

                                                else -> {

                                                    val serialized = gson.toJson(value)
                                                    out?.value(serialized)
                                                }
                                            }

                                        } catch (e: Exception) {

                                            Console.error("$tag ERROR: ${e.message}")
                                            recordException(e)
                                        }

                                        try {



                                            Console.log("$rwTag END")

                                        } catch (e: Exception) {

                                            Console.error("$rwTag ERROR: ${e.message}")
                                            recordException(e)
                                        }
                                    }

                                    fun customWrite() {

                                        Console.log(

                                            "$wTag Custom write :: START :: " +
                                                    "Class = '${clazz.canonicalName?.forClassName()}'"
                                        )

                                        recipe[fieldName]?.let { serializer ->

                                            if (serializer is DefaultCustomSerializer) {

                                                Console.log("$wTag Custom write :: Custom serializer")

                                                when (clazz.canonicalName?.forClassName()) {

                                                    "byte[]",
                                                    ByteArray::class.java.canonicalName?.forClassName() -> {

                                                        try {

                                                            val success = byteArraySerializer.serialize(

                                                                fieldName,
                                                                fValue
                                                            )

                                                            if (!success) {

                                                                throw IOException(

                                                                    "Could not serialize the '" +
                                                                            "$fieldName' for " +
                                                                            "'${instance::class.java.canonicalName?.forClassName()}'"
                                                                )
                                                            }

                                                        } catch (e: Exception) {

                                                            Console.error("$wTag ERROR: ${e.message}")
                                                            recordException(e)
                                                        }
                                                    }

                                                    else -> {

                                                        val e = IllegalArgumentException(

                                                            "Not supported type for default " +
                                                                    "custom serializer " +
                                                                    "'${clazz.canonicalName?.forClassName()}'"
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

                    fun customRead(fieldName: String): Any? {

                        val tag = "$tag CUSTOM ::"

                        Console.log("$tag START")

                        try {

                            recipe[fieldName]?.let { serializer ->

                                if (serializer is DefaultCustomSerializer) {

                                    val clazz = serializer.takeClass()

                                    Console.log(

                                        "$tag Custom write :: Custom serializer :: " +
                                                "Class = '${clazz.canonicalName?.forClassName()}'"
                                    )

                                    when (clazz.canonicalName?.forClassName()) {

                                        "byte[]",
                                        ByteArray::class.java.canonicalName?.forClassName() -> {

                                            val result =
                                                byteArraySerializer.deserialize(fieldName)

                                            return result
                                        }

                                        else -> {

                                            val e = IllegalArgumentException(

                                                "Not supported type for default " +
                                                        "custom serializer " +
                                                        "'${clazz.canonicalName?.forClassName()}'"
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

                    while (`in`?.hasNext() == true) {

                        val fieldName = `in`.nextName()

                        Console.log("$tag Field name = '$fieldName'")

                        val fieldClazz = clazz.getFieldByName(fieldName)?.type
                        val fieldCanonical = fieldClazz?.canonicalName?.forClassName()

                        if (isEmpty(fieldCanonical)) {

                            throw IllegalArgumentException(

                                "Could not find field '$fieldName' " +
                                    "for the '${clazz.canonicalName?.forClassName()}' class"
                            )
                        }

                        val tag = "$tag Field = '$fieldName' :: Field class = '$fieldCanonical' ::"

                        fieldsRead.add(fieldName)

                        if (recipe.containsKey(fieldName)) {

                            val read = customRead(fieldName)

                            Console.log("$tag Read = '$read'")

                            assign(instance, fieldName, read)

                            Console.log("$tag Assigned = '$read'")

                        } else {

                            fun regularRead(): Any? {

                                val tag = "$tag REGULAR ::"

                                Console.log("$tag START")

                                try {

                                    when (fieldCanonical) {

                                        Int::class.java.canonicalName?.forClassName() -> return `in`.nextInt()
                                        "int" -> return `in`.nextInt()
                                        "java.lang.Integer" -> return `in`.nextInt()
                                        Long::class.java.canonicalName?.forClassName() -> return `in`.nextLong()
                                        "long" -> return `in`.nextLong()
                                        "java.lang.Long" -> return `in`.nextLong()
                                        String::class.java.canonicalName?.forClassName() -> return `in`.nextString()
                                        "string" -> return `in`.nextString()
                                        "java.lang.String" -> return `in`.nextString()
                                        Double::class.java.canonicalName?.forClassName() -> return `in`.nextDouble()
                                        "double" -> return `in`.nextDouble()
                                        "java.lang.Double" -> return `in`.nextDouble()
                                        Float::class.java.canonicalName?.forClassName() -> return `in`.nextDouble()
                                        "float" -> return `in`.nextDouble()
                                        "java.lang.Float" -> return `in`.nextDouble()
                                        Boolean::class.java.canonicalName?.forClassName() -> return `in`.nextBoolean()
                                        "boolean" -> return `in`.nextBoolean()
                                        "java.lang.Boolean" -> return `in`.nextBoolean()

                                        else -> {

                                            val json = `in`.nextString()

                                            Console.log(

                                                "$tag JSON = '$json', " +
                                                        "Field canonical = '$fieldCanonical"
                                            )

                                            val result = gson.fromJson(json, fieldClazz)

                                            Console.log("$tag END: $result")

                                            return result
                                        }
                                    }

                                } catch (e: Exception) {

                                    Console.error("$tag ERROR: ${e.message}")
                                    recordException(e)
                                }

                                return null
                            }

                            val read = regularRead()

                            Console.log("$tag Read = '$read'")

                            assign(instance, fieldName, read)

                            Console.log("$tag Assigned = '$read'")
                        }
                    }

                    val fields = clazz.getAllFields()

                    fields.forEach { field ->

                        val fieldName = field.name

                        val tag = "$tag ADDITIONAL :: Field = '$fieldName' ::"

                        if (!fieldsRead.contains(fieldName) && !field.isExcluded(instance)) {

                            Console.log("$tag START")

                            val read = customRead(fieldName)

                            Console.log("$tag Read = '$read'")

                            assign(instance, fieldName, read)

                            Console.log("$tag Assigned = '$read'")

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

        Console.log("$tag INSTANTIATE :: Class = '${clazz.canonicalName?.forClassName()}' :: START")

        try {

            var instance: Any? = null

            if (clazz.hasPublicDefaultConstructor()) {

                instance = clazz.newInstance()

                Console.log("$tag INSTANTIATE :: END :: Instance = '$instance'")

            } else {

                Console.error(

                    "$tag INSTANTIATE :: END :: No public constructor " +
                            "found for class '${clazz.canonicalName?.forClassName()}'"
                )
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
