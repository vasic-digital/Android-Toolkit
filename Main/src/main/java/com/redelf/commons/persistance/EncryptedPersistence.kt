package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.ExclusionStrategy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redelf.commons.Erasing
import com.redelf.commons.R
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.obtain.Obtain
import timber.log.Timber

class EncryptedPersistence

@Throws(IllegalArgumentException::class)
constructor(

    ctx: Context,
    serializationExclusionStrategy: ExclusionStrategy? = null,
    deserializationExclusionStrategy: ExclusionStrategy? = null,

    private val keySalt: String = "st",
    private val storageTag: String = ctx.getString(R.string.app_name)

) : Persistence<String>, Erasing, TerminationSynchronized, InitializationWithContext {

    companion object {

        var DEBUG = true
        val logTag = "${Persistence.tag} Encrypted ::"
    }

    private var data: Data? = null

    init {

        Timber.v("$logTag :: Initialization :: Storage tag: '$storageTag'")

        val tag = "Exclusion strategies ::"

        ctx.let {

            val getParser = object : Obtain<Parser?> {

                override fun obtain(): Parser {

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


                    return GsonParser(

                        object : Obtain<Gson> {

                            override fun obtain(): Gson {

                                return gsonBuilder.create()
                            }
                        }
                    )
                }
            }

            val logger = PersistenceLogInterceptor

            val salter = object : Salter {

                override fun getSalt() = keySalt
            }

            data = PersistenceBuilder.instantiate(it, salter = salter, storageTag = storageTag)
                .setParser(getParser)
                .setLogInterceptor(logger)
                .setDoLog(true)
                .setLogRawData(true)
                .build()
        }
    }

    override fun shutdown(): Boolean {

        return data?.shutdown() ?: false
    }

    override fun initialize(ctx: Context) {

        data?.initialize(ctx)
    }

    override fun <T> pull(key: String): T? {

        return data?.get(key)
    }

    override fun <T> push(key: String, what: T): Boolean {

        return data?.put(key, what) ?: false
    }

    override fun delete(what: String): Boolean {

        return data?.delete(what) ?: false
    }

    override fun contains(key: String): Boolean {

        return data?.contains(key) ?: false
    }

    /*
         DANGER ZONE:
    */
    override fun erase(): Boolean {

        return data?.deleteAll() ?: false
    }
}