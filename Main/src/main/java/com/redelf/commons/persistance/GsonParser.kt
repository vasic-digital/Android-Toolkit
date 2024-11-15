package com.redelf.commons.persistance

import com.google.gson.Gson
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import java.lang.reflect.Type

class GsonParser(private val provider: Obtain<Gson>) : Parser {

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

        try {

            return provider.obtain().toJson(body)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }
}
