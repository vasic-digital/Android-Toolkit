package com.redelf.commons.persistance

import android.content.Context
import com.google.gson.GsonBuilder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.destruction.erasing.Erasing
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.extensions.yield
import com.redelf.commons.lifecycle.initialization.InitializationWithContext
import com.redelf.commons.lifecycle.shutdown.ShutdownSynchronized
import com.redelf.commons.lifecycle.termination.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Persistence
import com.redelf.commons.persistance.base.PersistenceAsync
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.registration.Registration
import com.redelf.commons.security.encryption.EncryptionListener
import java.util.concurrent.atomic.AtomicBoolean

class EncryptedPersistence
@Throws(IllegalArgumentException::class, IllegalStateException::class)
constructor(

    ctx: Context,

    private val keySalt: String = "st",
    private val storageTag: String = BaseApplication.getName(),
    private val doLog: Boolean = false,

) :

    Erasing,
    ShutdownSynchronized,
    TerminationSynchronized,
    PersistenceAsync<String>,
    InitializationWithContext,
    Registration<EncryptionListener<String, String>>

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

        ctx.let {

            val getParser = object : Obtain<Parser> {

                override fun obtain(): Parser {

                    return GsonParser.instantiate(

                        storageTag,
                        null,
                        true,
                        gsonBuilder
                    )
                }
            }

            val salter = object : Salter {

                override fun getSalt() = keySalt.hashCodeString()
            }

            dataDelegate = PersistenceBuilder.instantiate(

                it,
                salter = salter,
                storageTag = storageTag

            )
                .setParser(getParser)
                .setDoLog(doLog)
                .build()
        }
    }

    fun isEncryptionEnabled() = dataDelegate?.isEncryptionEnabled() == true

    fun isEncryptionDisabled() = !isEncryptionEnabled()

    override fun shutdown(): Boolean {

        return dataDelegate?.shutdown() == true
    }

    override fun terminate(vararg args: Any): Boolean {

        return dataDelegate?.terminate(*args) == true
    }

    override fun initialize(ctx: Context) {

        dataDelegate?.initialize(ctx)
    }

    override fun <T> pull(key: String, callback: OnObtain<T?>) {

        exec(

            onRejected = { e -> callback.onFailure(e) }

        ) {

            if (DEBUG.get()) {

                Console.log("$LOG_TAG :: Pull: key = '$key' ::")
            }

            try {

                val result = dataDelegate?.get<T?>(key, null)

                callback.onCompleted(result)

            } catch (e: Throwable) {

                Console.error(

                    "$LOG_TAG ERROR: Failed to pull data for key " +
                            "'$key', Error='${e.message}'"
                )

                callback.onFailure(e)
            }
        }
    }

    override fun <T> push(key: String, what: T, check: Obtain<Boolean>): Boolean {

        if (dataDelegate == null) {

            Console.error("$LOG_TAG ERROR: Data delegate instance is null")

            return false
        }

        yield("EncryptedPersistence.push.$key", check)

        return dataDelegate?.put(key, what) == true
    }

    override fun delete(what: String, callback: OnObtain<Boolean?>) {

        exec(

            onRejected = { e -> callback.onFailure(e) }

        ) {

            val result = dataDelegate?.delete(what)

            callback.onCompleted(result)
        }
    }

    override fun contains(key: String, callback: OnObtain<Boolean?>) {

        exec(

            onRejected = { e -> callback.onFailure(e) }

        ) {

            callback.onCompleted(dataDelegate?.contains(key) == true)
        }
    }

    /*
         DANGER ZONE:
    */
    override fun erase(): Boolean {

        return dataDelegate?.deleteAll() == true
    }

    override fun register(subscriber: EncryptionListener<String, String>) {

        dataDelegate?.register(subscriber)
    }

    override fun unregister(subscriber: EncryptionListener<String, String>) {

        dataDelegate?.unregister(subscriber)
    }

    override fun isRegistered(subscriber: EncryptionListener<String, String>): Boolean {

        return dataDelegate?.isRegistered(subscriber) == true
    }
}