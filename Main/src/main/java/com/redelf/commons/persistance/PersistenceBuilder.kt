package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.redelf.commons.obtain.Obtain
import timber.log.Timber

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

            Timber.i("Data :: Initializing")

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

    private var parser: Obtain<Parser?> = object : Obtain<Parser?> {

        override fun obtain() = GsonParser(

            object : Obtain<Gson> {

                override fun obtain(): Gson {

                    return Gson()
                }
            }
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
    var serializer: Serializer? = DataSerializer()
    var converter: Converter? = DataConverter(parser)
    var logInterceptor: LogInterceptor = PersistenceLogInterceptor
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

    fun setEncrypt(enc: Boolean): PersistenceBuilder {

        this.encrypt = enc
        return this
    }

    fun setParser(parser: Obtain<Parser?>): PersistenceBuilder {

        this.parser = parser
        return this
    }

    fun setSerializer(serializer: Serializer?): PersistenceBuilder {

        this.serializer = serializer
        return this
    }

    fun setLogInterceptor(logInterceptor: LogInterceptor): PersistenceBuilder {

        this.logInterceptor = logInterceptor
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

    fun build(): Data {

        return Data.instantiate(this)
    }
}