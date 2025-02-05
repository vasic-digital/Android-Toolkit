package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.persistance.base.Serializer
import com.redelf.commons.persistance.base.Storage

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

            /*
                TODO: Bring the Jackson support
            */
            return GsonBuilder()
        }
    }

    private var parser: Obtain<Parser> = object : Obtain<Parser> {

        override fun obtain() = GsonParser.instantiate(

            storageTag,
            encryption,
            true,
            pCallback
        )
    }

    var doLog: Boolean = false
    var storage: Storage<String> = DBStorage
    var encryption: Encryption<String>? = null
    var converter: Converter? = DataConverter(parser)
    var serializer: Serializer? = DataSerializer(parser)

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

            if (encryption is ConcealEncryption && (!(encryption as ConcealEncryption).init())) {

                throw IllegalStateException("Could not initialized Conceal encryption")
            }
        }

        return DataDelegate.instantiate(this)
    }

    private fun instantiateDefaultEncryption(context: Context, salter: Salter): Encryption<String> {

        return CompressedEncryption()
    }
}