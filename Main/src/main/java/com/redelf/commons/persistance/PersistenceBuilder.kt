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


package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.GsonBuilder
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.persistance.base.Serializer
import com.redelf.commons.persistance.base.Storage
import com.redelf.commons.persistance.database.DBStorage
import com.redelf.commons.persistance.encryption.NoEncryption

class PersistenceBuilder(

    private val context: Context,

    storageTag: String = "Data",

    private val  salter: Salter = object : Salter {

        override fun getSalt() = storageTag.hashCodeString().reversed()
    },

) {

    companion object {

        fun instantiate(

            context: Context,
            storageTag: String? = null,
            salter: Salter? = null

        ): PersistenceBuilder {

            Console.info("Data :: Initializing")

            if (!TextUtils.isEmpty(storageTag) && storageTag != null) {

                salter?.let {

                    return PersistenceBuilder(context, storageTag = storageTag, salter = it)
                }

                return PersistenceBuilder(context, storageTag = storageTag)
            }

            salter?.let {

                return PersistenceBuilder(context, salter = it)
            }

            return PersistenceBuilder(context)
        }
    }

    private val pCallback = object : Obtain<GsonBuilder> {
        override fun obtain(): GsonBuilder {
            return GsonBuilder()
        }
    }
    
    private val streamingCallback = object : Obtain<ObjectMapper> {
        override fun obtain(): ObjectMapper {
            // High-performance Jackson configuration
            return ObjectMapper().apply {
                configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            }
        }
    }

    private var parser: Obtain<Parser> = object : Obtain<Parser> {
        override fun obtain() = StreamingJsonParser.instantiate(
            storageTag,
            encryption,
            true,
            streamingCallback
        )
    }

    var doLog: Boolean = false
    var storage: Storage<String> = DBStorage.getInstance(context)
    var encryption: Encryption<String>? = null
    var converter: Converter? = SecureDataConverter(parser)
    var serializer: Serializer? = SecureDataSerializer(parser)

    fun setDoLog(doLog: Boolean): PersistenceBuilder {

        this.doLog = doLog
        return this
    }

    fun setParser(parser: Obtain<Parser>): PersistenceBuilder {

        this.parser = parser
        return this
    }

    fun setSerializer(serializer: Serializer?): PersistenceBuilder {

        this.serializer = serializer
        return this
    }

    fun setConverter(converter: Converter?): PersistenceBuilder {

        this.converter = converter
        return this
    }

    fun setEncryption(encryption: Encryption<String>?): PersistenceBuilder {

        this.encryption = encryption
        return this
    }

    @Throws(IllegalStateException::class)
    fun build(): DataDelegate {

        if (encryption == null) {

            encryption = instantiateDefaultEncryption(context, salter)
        }

        return DataDelegate.instantiate(this)
    }

    private fun instantiateDefaultEncryption(context: Context, salter: Salter): Encryption<String> {

        // FIXME:
        //  Pulling persisted data from the device tries to decrypt the partition data like
        //  partition count, which may not encrypted (we guess)
        //  see:
        //  return CompressedEncryption()

        return NoEncryption()
    }
}