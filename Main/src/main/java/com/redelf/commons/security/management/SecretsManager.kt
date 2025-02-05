package com.redelf.commons.security.management

import android.annotation.SuppressLint
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.data.type.Typed
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import com.redelf.commons.security.obfuscation.RemoteObfuscatorSaltProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
class SecretsManager private constructor(storageKeyToSet: String) :

    SingleInstantiated,
    ContextualManager<Secrets>()

{

    companion object : SingleInstance<SecretsManager>() {

        override fun instantiate(vararg params: Any): SecretsManager {

            val app = BaseApplication.takeContext()

            return SecretsManager(app.secretsKey)
        }
    }

    override val lazySaving = true
    override val instantiateDataObject = true

    override val typed = object : Typed<Secrets> {

        override fun getClazz(): Class<Secrets> = Secrets::class.java
    }

    override val storageKey = storageKeyToSet

    override fun getLogTag() = "SecretsManager :: ${hashCode()} ::"

    override fun createDataObject() = Secrets()

    fun getObfuscationSalt(source: RemoteObfuscatorSaltProvider): ObfuscatorSalt? {

        var result: ObfuscatorSalt? = null

        try {

            val data = obtain()
            val latch = CountDownLatch(1)

            result = data?.obfuscationSalt?: ObfuscatorSalt()

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

                            if (it.obfuscationSalt == null) {

                                it.obfuscationSalt = result
                            }

                            it.obfuscationSalt?.let { salt ->

                                result = salt
                            }

                            result?.updateValue(newSalt)

                            transaction.end()
                        }
                    }

                    latch.countDown()

                } catch (e: Exception) {

                    recordException(e)

                    result?.error = e

                    latch.countDown()
                }
            }

            if (isEmpty(result.takeValue())) {

                latch.await(60, TimeUnit.SECONDS)

                result.firstTimeObtained.set(true)

            } else {

                result.updateValue()

                result.firstTimeObtained.set(false)
            }

            return result

        } catch (e: Exception) {

            result?.error = e

            recordException(e)
        }

        return result
    }
}