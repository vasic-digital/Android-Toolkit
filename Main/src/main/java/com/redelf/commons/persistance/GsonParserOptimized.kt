package com.redelf.commons.persistance

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.GsonBuilder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.creation.instantiation.Instantiable
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optimized GsonParser that delegates to StreamingJsonParser for superior performance and safety.
 * Provides backward compatibility while leveraging modern streaming JSON capabilities.
 */
class GsonParserOptimized private constructor(
    encrypt: Boolean,
    encryption: Encryption<String>?,
    parserKey: String,
    provider: Obtain<GsonBuilder>
) : Parser {

    companion object : Instantiable<GsonParserOptimized> {

        val DEBUG = AtomicBoolean()
        
        @Deprecated("Use StreamingJsonParser directly for better performance")
        private val instances = ConcurrentHashMap<String, GsonParserOptimized>()
        
        private fun createObjectMapperProvider(gsonProvider: Obtain<GsonBuilder>): Obtain<ObjectMapper> {
            return object : Obtain<ObjectMapper> {
                override fun obtain(): ObjectMapper {
                    return ObjectMapper().apply {
                        // Configure to match Gson's behavior as closely as possible
                        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        override fun instantiate(vararg params: Any): GsonParserOptimized {
            if (params.size < 4) {
                throw IllegalArgumentException("Encryption parameters, key and provider expected")
            }

            return try {
                val key = params[2] as String? ?: ""
                val encrypt = params[0] as Boolean? == true
                val encryption = params[1] as Encryption<String>?
                val provider = params[3] as Obtain<GsonBuilder>?

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
            provider: Obtain<GsonBuilder>?
        ): GsonParserOptimized {
            if (provider == null) {
                throw IllegalArgumentException("Provider parameter is mandatory")
            }

            var mapKey = "$key.${provider.hashCode()}.$encrypt"
            encryption?.let {
                mapKey += ".${encryption::class.simpleName}"
            }

            instances[mapKey]?.let {
                return it
            }

            val parser = GsonParserOptimized(encrypt, encryption, key, provider)
            instances[mapKey] = parser
            
            if (DEBUG.get()) {
                Console.log("Created optimized GsonParser with StreamingJsonParser backend")
            }
            
            return parser
        }
        
        fun clearInstances() {
            instances.clear()
        }
    }

    // Delegate to StreamingJsonParser for all operations
    private val streamingParser = StreamingJsonParser.instantiate(
        parserKey,
        encryption,
        encrypt,
        createObjectMapperProvider(provider)
    )
    
    private val tag = "GsonParserOptimized :: Key='$parserKey', Hash='${hashCode()}' ::"

    override fun toJson(body: Any?): String? {
        if (DEBUG.get()) {
            Console.log("$tag Delegating serialization to StreamingJsonParser")
        }
        
        return try {
            streamingParser.toJson(body)
        } catch (e: Throwable) {
            Console.error("$tag Serialization delegation failed: ${e.message}")
            recordException(e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, type: Type?): T? {
        if (DEBUG.get()) {
            Console.log("$tag Delegating type-based deserialization to StreamingJsonParser")
        }
        
        return try {
            streamingParser.fromJson<T>(content, type)
        } catch (e: Throwable) {
            Console.error("$tag Type-based deserialization delegation failed: ${e.message}")
            recordException(e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(content: String?, clazz: Class<*>?): T? {
        if (DEBUG.get()) {
            Console.log("$tag Delegating class-based deserialization to StreamingJsonParser")
        }
        
        return try {
            streamingParser.fromJson<T>(content, clazz)
        } catch (e: Throwable) {
            Console.error("$tag Class-based deserialization delegation failed: ${e.message}")
            recordException(e)
            null
        }
    }
    
    /**
     * Get performance metrics from the underlying StreamingJsonParser
     */
    fun getPerformanceMetrics(): Map<String, Long> {
        return StreamingJsonParser.getPerformanceMetrics()
    }
    
    /**
     * Cleanup resources when parser is no longer needed
     */
    fun cleanup() {
        if (DEBUG.get()) {
            Console.log("$tag Cleaning up optimized GsonParser resources")
        }
    }
}