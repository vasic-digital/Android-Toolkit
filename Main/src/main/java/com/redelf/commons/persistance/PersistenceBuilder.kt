package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.persistance.base.Serializer
import com.redelf.commons.persistance.base.Storage
import java.util.concurrent.CopyOnWriteArrayList

class PersistenceBuilder(

    context: Context,
    storageTag: String = "Data",

    salter: Salter = object : Salter {

        override fun getSalt() = storageTag
    },

    private var encrypt: Boolean = false

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

    private fun instantiateDefaultEncryption(context: Context, salter: Salter): Encryption {

        if (encrypt) {

            return ConcealEncryption(context, salter)
        }

        return NoEncryption()
    }

    var doLog: Boolean = false
    var logRawData: Boolean = false
    var storage: Storage<String> = DBStorage
    var converter: Converter? = DataConverter(parser)
    var serializer: Serializer? = DataSerializer(parser)
    var keysFilter: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()
    var encryption: Encryption? = instantiateDefaultEncryption(context, salter)

    init {

        if (encryption is ConcealEncryption && (!(encryption as ConcealEncryption).init())) {

            encryption = NoEncryption()
        }
    }

    fun setDoLog(doLog: Boolean): PersistenceBuilder {

        this.doLog = doLog
        return this
    }

    fun setLogRawData(logRawData: Boolean): PersistenceBuilder {

        this.logRawData = logRawData
        return this
    }

    fun addKeysFilter(filter: String): PersistenceBuilder {

        this.keysFilter.add(filter)
        return this
    }

    fun addKeysFilters(filters: List<String>): PersistenceBuilder {

        this.keysFilter.addAll(filters)
        return this
    }

    fun setEncrypt(enc: Boolean): PersistenceBuilder {

        this.encrypt = enc
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

    fun setEncryption(encryption: Encryption?): PersistenceBuilder {

        this.encryption = encryption
        return this
    }

    fun build(): DataDelegate {

        return DataDelegate.instantiate(this)
    }
}