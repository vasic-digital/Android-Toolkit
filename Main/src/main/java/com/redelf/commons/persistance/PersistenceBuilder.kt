package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.redelf.commons.instantiation.Instantiable
import com.redelf.commons.obtain.Obtain
import timber.log.Timber

class PersistenceBuilder(

    context: Context,
    storageTag: String = "Data",

    salter: Salter = object : Salter {

        override fun getSalt() = storageTag
    }

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

    var storage: Storage<String> = DBStorage
    var serializer: Serializer? = DataSerializer()
    var converter: Converter? = DataConverter(parser)
    var logInterceptor: LogInterceptor = LogInterceptor { }
    var encryption: Encryption? = ConcealEncryption(context, salter)

    init {

        if (!(encryption as ConcealEncryption).init()) {

            encryption = NoEncryption()
        }
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