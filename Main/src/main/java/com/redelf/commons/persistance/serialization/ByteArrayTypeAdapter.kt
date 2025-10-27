/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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