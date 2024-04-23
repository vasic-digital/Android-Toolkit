package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.Gson
import com.redelf.commons.obtain.Obtain

class PersistenceBuilder(

    context: Context,
    storageTag: String = "Data",

    salter: Salter = object : Salter {

        override fun getSalt() = storageTag
    }

) {

    private var parser: Obtain<Parser?> = object : Obtain<Parser?> {

        override fun obtain() = GsonParser(

            object : Obtain<Gson> {

                override fun obtain(): Gson {

                    return Gson()
                }
            }
        )
    }

    var serializer: Serializer? = DataSerializer()
    var storage: Storage<String> = DBStorage(context)
    var converter: Converter? = DataConverter(parser)
    var logInterceptor: LogInterceptor = LogInterceptor { }
    var encryption: Encryption? = ConcealEncryption(context, salter)

    init {

        PersistenceUtils.checkNull("Context", context)

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

    fun build() {

        Data.build(this)
    }
}