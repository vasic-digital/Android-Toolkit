package com.redelf.commons.persistance.serialization

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

class ByteArrayTypeAdapter(

    context: Context,
    private val name: String,
    encryption: Boolean = true

) : TypeAdapter<ByteArray>() {

    /*
    * TODO:
    *  - Encrypt all strings used here (name for example ...)
    */
    private val serializer = SecureBinarySerializer(context, "type_adapter_cache.$name", encryption)

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: ByteArray?) {

        if (value == null) {

            out.nullValue()

        } else {

            out.value(serializer.serialize(name, value))
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): ByteArray? {

        if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            return null
        }

        val encoded = `in`.nextString()
        
        return if (encoded != null) {
            // The serializer stores the data and returns the ByteArray
            serializer.deserialize(name) as? ByteArray
        } else {
            null
        }
    }
}