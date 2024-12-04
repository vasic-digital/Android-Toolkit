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
    private val serializer = ByteArraySerializer(context, "type_adapter_cache.$name", encryption)

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

        val encoded = `in`.nextBoolean() == true

        if (encoded) {

            return serializer.deserialize(name)
        }

        return null
    }
}