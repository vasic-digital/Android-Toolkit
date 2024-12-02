package com.redelf.commons.persistance

import com.google.gson.Gson
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.serialization.CustomSerializable
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

class GsonParser(private val provider: Obtain<Gson>) : Parser {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "Parser :: GSON ::"

    override fun <T> fromJson(content: String?, type: Type?): T? {

        try {

            if (isEmpty(content)) {

                return null
            }

            return provider.obtain().fromJson(content, type)

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

            return provider.obtain().fromJson(content, clazz)

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

        if (DEBUG.get()) Console.log("$tag START :: Class = ${body::class.java.canonicalName}")

        try {

            val gsonProvider = provider.obtain()

            if (body is CustomSerializable) {

                val customizations = body.getCustomSerializations()

                if (DEBUG.get()) {

                    Console.log(

                        "$tag Custom serialization :: " +
                                "Class = ${body::class.java.canonicalName}, " +
                                "Customizations = $customizations"
                    )
                }

                // TODO: Apply customizations
            }

            return gsonProvider.toJson(body)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }
}
