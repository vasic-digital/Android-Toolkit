package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.ExclusionStrategy
import com.google.gson.GsonBuilder
import com.redelf.commons.Erasing
import com.redelf.commons.R
import timber.log.Timber

class EncryptedPersistence

@Throws(IllegalArgumentException::class)
constructor(

    ctx: Context? = null,
    serializationExclusionStrategy: ExclusionStrategy? = null,
    deserializationExclusionStrategy: ExclusionStrategy? = null,

    private val keySalt: String = "",
    private val storageTag: String = ctx?.getString(R.string.app_name) ?: "storage"

) : Persistence<String>, Erasing {

    companion object {

        var DEBUG = true
    }

    init {

        val tag = "Exclusion strategies ::"

        val err = IllegalArgumentException(

            "Context is required with combination " +
                    "with the exclusion strategies"
        )

        serializationExclusionStrategy?.let {

            if (ctx == null) {

                throw err
            }
        }

        deserializationExclusionStrategy?.let {

            if (ctx == null) {

                throw err
            }
        }

        ctx?.let {

            val gsonBuilder = GsonBuilder()
                .enableComplexMapKeySerialization()

            if (serializationExclusionStrategy == deserializationExclusionStrategy) {

                serializationExclusionStrategy?.let {

                    Timber.v("$tag Exclusion Strategies: $serializationExclusionStrategy")

                    gsonBuilder.setExclusionStrategies(serializationExclusionStrategy)
                }

            } else {

                serializationExclusionStrategy?.let {

                    Timber.v("$tag Ser. Excl. Strategy: $serializationExclusionStrategy")

                    gsonBuilder.addSerializationExclusionStrategy(serializationExclusionStrategy)
                }

                deserializationExclusionStrategy?.let {

                    Timber.v("$tag De-Ser. Excl. Strategy: $serializationExclusionStrategy")

                    gsonBuilder.addSerializationExclusionStrategy(deserializationExclusionStrategy)
                }
            }

            val gson = gsonBuilder
                .create()

            val parser = GsonParser(gson)

            val logger =
                LogInterceptor { message ->

                    if (DEBUG) {

                        Timber.v(message)
                    }
                }

            val salter = object : Salter {

                override fun getSalt() = keySalt
            }

            Data.init(it, salter = salter, storageTag = storageTag)
                .setParser(parser)
                .setLogInterceptor(logger)
                .build()
        }
    }

    override fun <T> pull(key: String): T? {

        return Data[key]
    }

    override fun <T> push(key: String, what: T): Boolean {

        return Data.put(key, what)
    }

    override fun delete(what: String): Boolean {

        return Data.delete(what)
    }

    override fun erase(): Boolean {

        return Data.deleteAll()
    }

    fun deleteKeysWithPrefix(value: String): Boolean {

        return Data.deleteKeysWithPrefix(value)
    }
}