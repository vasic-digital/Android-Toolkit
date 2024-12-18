package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.ExclusionStrategy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.destruction.erasing.Erasing
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Persistence
import com.redelf.commons.persistance.base.Salter
import java.util.concurrent.atomic.AtomicBoolean

class EncryptedPersistence
@Throws(IllegalArgumentException::class)
constructor(

    ctx: Context,
    serializationExclusionStrategy: ExclusionStrategy? = null,
    deserializationExclusionStrategy: ExclusionStrategy? = null,

    private val keySalt: String = "st",
    private val doEncrypt: Boolean = true,
    private val storageTag: String = BaseApplication.getName(),

    private val doLog: Boolean = false,
    private val logRawData: Boolean = false,
    private val logStorageKeys: List<String> = emptyList()

) :

    Erasing,
    Persistence<String>,
    ShutdownSynchronized,
    TerminationSynchronized,
    InitializationWithContext

{

    companion object {

        val DEBUG = AtomicBoolean()

        const val LOG_TAG = "${Persistence.TAG} Encrypted ::"
    }

    private var dataDelegate: DataDelegate? = null

    private val gsonBuilder = object : Obtain<GsonBuilder> {

        override fun obtain(): GsonBuilder {

            return GsonBuilder()
                .enableComplexMapKeySerialization()
        }
    }

    init {

        if (DEBUG.get()) Console.log(

            "$LOG_TAG :: Initialization :: Storage tag: '$storageTag'"
        )

        val tag = "Exclusion strategies ::"

        ctx.let {

            val getParser = object : Obtain<Parser> {

                override fun obtain(): Parser {

                    val gsonBuilder = this@EncryptedPersistence.gsonBuilder.obtain()

                    if (serializationExclusionStrategy == deserializationExclusionStrategy) {

                        serializationExclusionStrategy?.let {

                            if (DEBUG.get()) Console.log(

                                "$tag Exclusion Strategies: $serializationExclusionStrategy"
                            )

                            gsonBuilder.setExclusionStrategies(serializationExclusionStrategy)
                        }

                    } else {

                        serializationExclusionStrategy?.let {

                            if (DEBUG.get()) Console.log(

                                "$tag Ser. Excl. Strategy: $serializationExclusionStrategy"
                            )

                            gsonBuilder.addSerializationExclusionStrategy(
                                serializationExclusionStrategy
                            )
                        }

                        deserializationExclusionStrategy?.let {

                            if (DEBUG.get()) Console.log(

                                "$tag De-Ser. Excl. Strategy: $serializationExclusionStrategy"
                            )

                            gsonBuilder.addSerializationExclusionStrategy(
                                deserializationExclusionStrategy
                            )
                        }
                    }

                    return GsonParser.instantiate(

                        storageTag,
                        gsonBuilder
                    )
                }
            }

            val salter = object : Salter {

                override fun getSalt() = keySalt
            }

            dataDelegate = PersistenceBuilder.instantiate(

                it,
                salter = salter,
                storageTag = storageTag

            )
                .setParser(getParser)
                .setDoLog(doLog)
                .setEncrypt(doEncrypt)
                .setLogRawData(logRawData)
                .addKeysFilters(logStorageKeys)
                .build()
        }
    }

    fun isEncryptionEnabled() = doEncrypt

    fun isEncryptionDisabled() = !doEncrypt

    override fun shutdown(): Boolean {

        return dataDelegate?.shutdown() == true
    }

    override fun terminate(vararg args: Any): Boolean {

        return dataDelegate?.terminate(*args) == true
    }

    override fun initialize(ctx: Context) {

        dataDelegate?.initialize(ctx)
    }

    override fun <T> pull(key: String): T? {

        if (DEBUG.get()) {

            Console.log("$LOG_TAG :: Pull: key = '$key' ::")
        }

        return dataDelegate?.get(key)
    }

    override fun <T> push(key: String, what: T): Boolean {

        if (dataDelegate == null) {

            Console.error("$LOG_TAG ERROR: Data delegate instance is null")

            return false
        }

        return dataDelegate?.put(key, what) == true
    }

    override fun delete(what: String): Boolean {

        return dataDelegate?.delete(what) == true
    }

    override fun contains(key: String): Boolean {

        return dataDelegate?.contains(key) == true
    }

    /*
         DANGER ZONE:
    */
    override fun erase(): Boolean {

        return dataDelegate?.deleteAll() == true
    }
}