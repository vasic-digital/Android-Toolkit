package com.redelf.commons.security.management

import android.annotation.SuppressLint
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.data.type.Typed
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.security.obfuscation.RemoteObfuscatorSaltProvider
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
class SecretsManager private constructor() : ContextualManager<Secrets>() {

    companion object : SingleInstance<SecretsManager>() {

        override fun instantiate(): SecretsManager {

            return SecretsManager()
        }
    }

    override val lazySaving = true
    override val instantiateDataObject = true

    override val typed = object : Typed<Secrets> {

        override fun getClazz(): Class<Secrets> = Secrets::class.java
    }

    override val storageKey = "s3_cR3_tZ"

    override fun getLogTag() = "SecretsManager :: ${hashCode()} ::"

    override fun createDataObject() = Secrets()

    fun getObfuscationSalt(source: RemoteObfuscatorSaltProvider): ObfuscatorSalt {

        val result = ObfuscatorSalt()

        try {

            val data = obtain()
            val latch = CountDownLatch(1)

            exec(

                onRejected = { err ->

                    recordException(err)
                    latch.countDown()
                }

            ) {

                val transaction = transaction("setObfuscationSalt")

                try {

                    data?.let {

                        val newSalt = source.getRemoteData()

                        if (isNotEmpty(newSalt)) {

                            it.obfuscationSalt = newSalt

                            transaction.end()
                        }
                    }

                    latch.countDown()

                } catch (e: Exception) {

                    recordException(e)

                    result.error = e

                    latch.countDown()
                }
            }

            if (data?.obfuscationSalt.isNullOrEmpty()) {

                latch.await(60, TimeUnit.SECONDS)

                result.firstTimeObtained.set(true)
            }

            result.updateValue(data?.obfuscationSalt ?: "")

            return result

        } catch (e: Exception) {

            result.error = e

            recordException(e)
        }

        return result
    }
}