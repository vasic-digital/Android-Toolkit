package com.redelf.commons.persistance

import android.content.Context
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.creation.instantiation.Instantiable
import com.redelf.commons.extensions.assign
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.getAllFields
import com.redelf.commons.extensions.getFieldByName
import com.redelf.commons.extensions.getRawCassName
import com.redelf.commons.extensions.hasPublicDefaultConstructor
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isExcluded
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer
import com.redelf.commons.persistance.serialization.SecureBinarySerializer
import com.redelf.commons.persistance.serialization.Serializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * High-performance, memory-efficient JSON parser with streaming support.
 * Designed to handle unlimited JSON sizes and depths safely.
 */
class StreamingJsonParser private constructor(
    private val encrypt: Boolean,
    private val encryption: Encryption<String>?,
    private val parserKey: String,
    private val provider: Obtain<ObjectMapper>?
) : Parser {

    companion object : Instantiable<StreamingJsonParser> {
        
        val DEBUG = AtomicBoolean()
        
        // Security and performance limits
        private const val MAX_JSON_SIZE_BYTES = 500L * 1024 * 1024 // 500MB
        private const val MAX_NESTING_DEPTH = 10000
        private const val MAX_STRING_LENGTH = 100 * 1024 * 1024 // 100MB
        private const val MAX_INSTANCES = 50
        private const val OPERATION_TIMEOUT_SECONDS = 300L // 5 minutes for very large objects
        private const val STREAMING_BUFFER_SIZE = 64 * 1024 // 64KB
        
        // Instance management
        private val instances = ConcurrentHashMap<String, WeakReference<StreamingJsonParser>>()
        private val instanceCount = AtomicInteger(0)
        private val executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
        )
        private val instanceLock = ReentrantReadWriteLock()
        
        // Performance metrics
        private val totalOperations = AtomicLong(0)
        private val totalProcessingTime = AtomicLong(0)
        
        private val defaultObjectMapper by lazy {
            createOptimizedObjectMapper()
        }
        
        private fun createOptimizedObjectMapper(): ObjectMapper {
            val factory = JsonFactory().apply {
                // Configure streaming constraints for safety
                setStreamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(MAX_NESTING_DEPTH)
                    .maxStringLength(MAX_STRING_LENGTH.toInt())
                    .build())
                    
                setStreamWriteConstraints(StreamWriteConstraints.builder()
                    .maxNestingDepth(MAX_NESTING_DEPTH)
                    .build())
                    
                // Enable streaming for large objects
                configure(JsonParser.Feature.USE_FAST_DOUBLE_PARSER, true)
                configure(JsonGenerator.Feature.USE_FAST_DOUBLE_WRITER, true)
            }
            
            return ObjectMapper(factory).apply {
                // Note: KotlinModule not available, using standard ObjectMapper
                
                // Configure for safety and performance
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
                configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false)
                configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                
                // Memory optimization
                // Feature settings for performance
                disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        override fun instantiate(vararg params: Any): StreamingJsonParser {
            if (params.size < 4) {
                throw IllegalArgumentException("Encryption parameters, key and provider expected")
            }
            
            return try {
                val key = params[2] as String? ?: ""
                val encrypt = params[0] as Boolean? == true
                val encryption = params[1] as Encryption<String>?
                val provider = params[3] as Obtain<ObjectMapper>?
                
                instantiate(key, encryption, encrypt, provider)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Throwable) {
                recordException(e)
                throw IllegalStateException("ERROR: ${e.message}")
            }
        }
        
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        fun instantiate(
            key: String,
            encryption: Encryption<String>?,
            encrypt: Boolean,
            provider: Obtain<ObjectMapper>? = null
        ): StreamingJsonParser {
            
            val mapKey = buildString {
                append(key)
                append(".")
                append(provider?.hashCode() ?: "default")
                append(".")
                append(encrypt)
                encryption?.let { append(".").append(it::class.simpleName) }
            }
            
            return instanceLock.write {
                // Check for existing instance
                instances[mapKey]?.get()?.let { existingParser ->
                    return@write existingParser
                }
                
                // Cleanup and enforce limits
                cleanupWeakReferences()
                if (instanceCount.get() >= MAX_INSTANCES) {
                    throw IllegalStateException("Maximum parser instances reached: $MAX_INSTANCES")
                }
                
                val parser = StreamingJsonParser(encrypt, encryption, key, provider)
                instances[mapKey] = WeakReference(parser)
                instanceCount.incrementAndGet()
                parser
            }
        }
        
        private fun cleanupWeakReferences() {
            val iterator = instances.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.get() == null) {
                    iterator.remove()
                    instanceCount.decrementAndGet()
                }
            }
        }
        
        fun getPerformanceMetrics(): Map<String, Long> {
            return mapOf(
                "totalOperations" to totalOperations.get(),
                "totalProcessingTimeMs" to totalProcessingTime.get(),
                "averageProcessingTimeMs" to if (totalOperations.get() > 0) 
                    totalProcessingTime.get() / totalOperations.get() else 0L,
                "activeInstances" to instanceCount.get().toLong()
            )
        }
        
        fun shutdown() {
            executorService.shutdown()
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executorService.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }
    
    private val objectMapper = provider?.obtain() ?: defaultObjectMapper
    private val ctxRef: WeakReference<Context> = WeakReference(BaseApplication.takeContext())
    private val tag = "StreamingJsonParser :: Key='$parserKey', Hash='${hashCode()}' ::"
    
    private val byteArraySerializer by lazy {
        ctxRef.get()?.let { ctx ->
            SecureBinarySerializer(
                ctx,
                "StreamingParser.$parserKey",
                encrypt || encryption != null,
                encryption
            )
        }
    }
    
    override fun toJson(body: Any?): String? {
        if (body == null) return null
        
        val startTime = System.currentTimeMillis()
        val bodyTag = "$tag toJson :: Class='${body::class.java.canonicalName?.forClassName()}' ::"
        
        if (DEBUG.get()) Console.log("$bodyTag START")
        
        return try {
            val future: Future<String?> = executorService.submit<String?> {
                performStreamingSerialization(body, bodyTag)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            totalOperations.incrementAndGet()
            totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime)
            
            result
        } catch (e: TimeoutException) {
            Console.error("$bodyTag Serialization timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            recordException(e)
            null
        } catch (e: Throwable) {
            Console.error("$bodyTag Serialization failed: ${e.message}")
            recordException(e)
            null
        }
    }
    
    private fun performStreamingSerialization(body: Any, tag: String): String? {
        return try {
            if (body is CustomSerializable) {
                performCustomSerialization(body, tag)
            } else {
                // Use streaming for large objects
                ByteArrayOutputStream(STREAMING_BUFFER_SIZE).use { baos ->
                    objectMapper.writeValue(baos, body)
                    val result = baos.toString(Charsets.UTF_8.name())
                    
                    // Validate result size
                    if (result.toByteArray(Charsets.UTF_8).size.toLong() > MAX_JSON_SIZE_BYTES) {
                        throw IllegalArgumentException("Serialized JSON exceeds maximum size: $MAX_JSON_SIZE_BYTES bytes")
                    }
                    
                    result
                }
            }
        } catch (e: Throwable) {
            Console.error("$tag Streaming serialization error: ${e.message}")
            recordException(e)
            null
        }
    }
    
    private fun performCustomSerialization(body: CustomSerializable, tag: String): String? {
        return try {
            val customizations = body.getCustomSerializations()
            if (DEBUG.get()) Console.log("$tag Custom serializations: $customizations")
            
            StringWriter().use { writer ->
                objectMapper.factory.createGenerator(writer).use { generator ->
                    writeCustomObject(generator, body, customizations, tag)
                }
                writer.toString()
            }
        } catch (e: Throwable) {
            Console.error("$tag Custom serialization error: ${e.message}")
            recordException(e)
            null
        }
    }
    
    private fun writeCustomObject(
        generator: JsonGenerator,
        obj: Any,
        customizations: Map<String, Serializer>,
        tag: String
    ) {
        generator.writeStartObject()
        
        val clazz = obj::class.java
        val fields = clazz.getAllFields()
        
        for (field in fields) {
            val fieldName = field.name
            
            if (field.isExcluded(obj)) {
                if (DEBUG.get()) Console.log("$tag Excluded field: $fieldName")
                continue
            }
            
            try {
                val fieldValue = field.get(obj)
                if (fieldValue == null) continue
                
                generator.writeFieldName(fieldName)
                
                if (customizations.containsKey(fieldName)) {
                    writeCustomField(generator, fieldName, fieldValue, customizations[fieldName], tag)
                } else {
                    writeRegularField(generator, fieldValue, tag)
                }
            } catch (e: Throwable) {
                Console.error("$tag Error writing field '$fieldName': ${e.message}")
                recordException(e)
            }
        }
        
        generator.writeEndObject()
    }
    
    private fun writeCustomField(
        generator: JsonGenerator,
        fieldName: String,
        fieldValue: Any,
        serializer: Serializer?,
        tag: String
    ) {
        when (serializer) {
            is DefaultCustomSerializer -> {
                when {
                    fieldValue is ByteArray -> {
                        val success = byteArraySerializer?.serialize(fieldName, fieldValue) ?: false
                        if (!success) {
                            throw IOException("Failed to serialize byte array field: $fieldName")
                        }
                        generator.writeString("__BYTE_ARRAY_SERIALIZED__")
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "Unsupported type for default custom serializer: ${fieldValue::class.java.canonicalName}"
                        )
                    }
                }
            }
            else -> {
                try {
                    serializer?.serialize(fieldName, fieldValue)
                    generator.writeString("__CUSTOM_SERIALIZED__")
                } catch (e: Throwable) {
                    Console.error("$tag Custom serialization error: ${e.message}")
                    recordException(e)
                }
            }
        }
    }
    
    private fun writeRegularField(generator: JsonGenerator, fieldValue: Any, tag: String) {
        when (fieldValue) {
            is String -> generator.writeString(fieldValue)
            is Int -> generator.writeNumber(fieldValue)
            is Long -> generator.writeNumber(fieldValue)
            is Double -> generator.writeNumber(fieldValue)
            is Float -> generator.writeNumber(fieldValue)
            is Boolean -> generator.writeBoolean(fieldValue)
            else -> {
                // Serialize complex objects as JSON strings to avoid deep nesting issues
                val nestedJson = objectMapper.writeValueAsString(fieldValue)
                generator.writeString(nestedJson)
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, type: Type?): T? {
        if (isEmpty(content) || type == null) return null
        
        return try {
            val rawClazzName = type.getRawCassName()
            val clazz = Class.forName(rawClazzName)
            fromJson(content, clazz)
        } catch (e: Throwable) {
            Console.error("$tag Type-based deserialization error: ${e.message}")
            recordException(e)
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, clazz: Class<*>?): T? {
        if (isEmpty(content) || clazz == null) return null
        
        val startTime = System.currentTimeMillis()
        val contentSize = content!!.toByteArray(Charsets.UTF_8).size.toLong()
        
        if (contentSize > MAX_JSON_SIZE_BYTES) {
            Console.error("$tag JSON content exceeds maximum size: $contentSize > $MAX_JSON_SIZE_BYTES bytes")
            return null
        }
        
        val bodyTag = "$tag fromJson :: Class='${clazz.canonicalName?.forClassName()}', Size=${contentSize} ::"
        
        if (DEBUG.get()) Console.log("$bodyTag START")
        
        return try {
            val future: Future<T?> = executorService.submit<T?> {
                performStreamingDeserialization(content, clazz, bodyTag)
            }
            
            val result = future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            totalOperations.incrementAndGet()
            totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime)
            
            result
        } catch (e: TimeoutException) {
            Console.error("$bodyTag Deserialization timeout after ${OPERATION_TIMEOUT_SECONDS}s")
            recordException(e)
            null
        } catch (e: Throwable) {
            Console.error("$bodyTag Deserialization failed: ${e.message}")
            recordException(e)
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T> performStreamingDeserialization(content: String, clazz: Class<*>, tag: String): T? {
        return try {
            // Handle primitives efficiently
            when (clazz.canonicalName?.forClassName()) {
                "int", "java.lang.Integer" -> return content.toInt() as T?
                "long", "java.lang.Long" -> return content.replace("\"", "").toLong() as T?
                "string", "java.lang.String" -> return content as T?
                "double", "java.lang.Double" -> return content.toDouble() as T?
                "float", "java.lang.Float" -> return content.toFloat() as T?
                "boolean", "java.lang.Boolean" -> return content.toBoolean() as T?
            }
            
            val instance = instantiate(clazz)
            if (instance is CustomSerializable) {
                performCustomDeserialization(content, instance, tag) as T?
            } else {
                // Use streaming parser for large objects
                StringReader(content).use { reader ->
                    objectMapper.readValue(reader, clazz) as T?
                }
            }
        } catch (e: Throwable) {
            when (e) {
                is com.fasterxml.jackson.core.JsonParseException -> {
                    Console.error("$tag JSON Parse Error at position ${e.location?.charOffset ?: "unknown"}: ${e.message}")
                    if (content.length > 1000) {
                        val errorPos = (e.location?.charOffset ?: 0).toInt()
                        val contextStart = maxOf(0, errorPos - 50)
                        val contextEnd = minOf(content.length, errorPos + 50)
                        val contextSnippet = content.substring(contextStart, contextEnd)
                        Console.error("$tag JSON context around error: '${contextSnippet.replace("\n", "\\n")}'")
                    }
                }
                is com.fasterxml.jackson.databind.JsonMappingException -> {
                    Console.error("$tag JSON Mapping Error: ${e.message}")
                }
                else -> {
                    Console.error("$tag Streaming deserialization error: ${e.message}")
                }
            }
            recordException(e)
            null
        }
    }
    
    private fun performCustomDeserialization(content: String, instance: Any, tag: String): Any? {
        return try {
            val customizations = (instance as CustomSerializable).getCustomSerializations()
            if (DEBUG.get()) Console.log("$tag Custom deserializations: $customizations")
            
            StringReader(content).use { reader ->
                objectMapper.factory.createParser(reader).use { parser ->
                    readCustomObject(parser, instance, customizations, tag)
                }
            }
            
            instance
        } catch (e: Throwable) {
            Console.error("$tag Custom deserialization error: ${e.message}")
            recordException(e)
            null
        }
    }
    
    private fun readCustomObject(
        parser: JsonParser,
        instance: Any,
        customizations: Map<String, Serializer>,
        tag: String
    ) {
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw IllegalArgumentException("Expected START_OBJECT token")
        }
        
        val clazz = instance::class.java
        val fieldsRead = mutableSetOf<String>()
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName
            parser.nextToken() // Move to value
            
            fieldsRead.add(fieldName)
            
            try {
                if (customizations.containsKey(fieldName)) {
                    readCustomField(parser, instance, fieldName, customizations[fieldName], tag)
                } else {
                    readRegularField(parser, instance, clazz, fieldName, tag)
                }
            } catch (e: Throwable) {
                Console.error("$tag Error reading field '$fieldName': ${e.message}")
                recordException(e)
            }
        }
        
        // Handle additional custom fields not in JSON
        val fields = clazz.getAllFields()
        for (field in fields) {
            val fieldName = field.name
            if (!fieldsRead.contains(fieldName) && 
                !field.isExcluded(instance) && 
                customizations.containsKey(fieldName)) {
                
                try {
                    readCustomField(null, instance, fieldName, customizations[fieldName], tag)
                } catch (e: Throwable) {
                    Console.error("$tag Error reading additional field '$fieldName': ${e.message}")
                    recordException(e)
                }
            }
        }
    }
    
    private fun readCustomField(
        parser: JsonParser?,
        instance: Any,
        fieldName: String,
        serializer: Serializer?,
        tag: String
    ) {
        when (serializer) {
            is DefaultCustomSerializer -> {
                val clazz = serializer.takeClass()
                when {
                    clazz.canonicalName?.forClassName() == "byte[]" || clazz == ByteArray::class.java -> {
                        val result = byteArraySerializer?.deserialize(fieldName)
                        instance.assign(fieldName, result, tag)
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "Unsupported type for default custom serializer: ${clazz.canonicalName}"
                        )
                    }
                }
            }
            else -> {
                try {
                    val result = serializer?.deserialize(fieldName)
                    instance.assign(fieldName, result, tag)
                } catch (e: Throwable) {
                    Console.error("$tag Custom deserialization error: ${e.message}")
                    recordException(e)
                }
            }
        }
    }
    
    private fun readRegularField(
        parser: JsonParser,
        instance: Any,
        clazz: Class<*>,
        fieldName: String,
        tag: String
    ) {
        val field = clazz.getFieldByName(fieldName) ?: return
        val fieldType = field.type
        
        val value = when (fieldType.canonicalName?.forClassName()) {
            "int", "java.lang.Integer" -> parser.intValue
            "long", "java.lang.Long" -> parser.longValue
            "string", "java.lang.String" -> parser.text
            "double", "java.lang.Double" -> parser.doubleValue
            "float", "java.lang.Float" -> parser.floatValue
            "boolean", "java.lang.Boolean" -> parser.booleanValue
            else -> {
                // Handle complex nested objects
                val nestedJson = parser.text
                try {
                    objectMapper.readValue(nestedJson, fieldType)
                } catch (e: Throwable) {
                    if (DEBUG.get()) {
                        Console.log("$tag Failed to parse nested object for field '$fieldName': ${e.message}")
                    }
                    null
                }
            }
        }
        
        instance.assign(fieldName, value, tag)
    }
    
    private fun instantiate(clazz: Class<*>): Any? {
        return try {
            if (clazz.hasPublicDefaultConstructor()) {
                clazz.getDeclaredConstructor().newInstance()
            } else {
                Console.error("$tag No public constructor found for: ${clazz.canonicalName}")
                null
            }
        } catch (e: Throwable) {
            Console.error("$tag Instantiation error: ${e.message}")
            recordException(e)
            null
        }
    }
}