package com.redelf.commons.persistance

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Timber
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

            Timber.e("Tried to deserialize into '${type?.typeName}' from '$content'")
        }

        return null
    }

    override fun toJson(body: Any?): String? {

        return provider.obtain().toJson(body)
    }
}
