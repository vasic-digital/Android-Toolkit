package com.redelf.commons.security.management

import android.annotation.SuppressLint
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.security.obfuscation.ObfuscatorSalt
import com.redelf.commons.security.obfuscation.RemoteObfuscatorSaltProvider

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

    override val storageKey = storageKeyToSet

    override fun getLogTag() = "SecretsManager :: ${hashCode()} ::"

    override fun createDataObject() = Secrets()

    fun getObfuscationSalt(

        source: RemoteObfuscatorSaltProvider,
        callback: OnObtain<ObfuscatorSalt?>

    ) {

        exec(

            onRejected = { e -> callback.onFailure(e) }

        ) {

            try {

                fun onSecrets(data: Secrets?) {

                    var result = data?.obfuscationSalt?: ObfuscatorSalt()
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

                                result.updateValue(newSalt)

                                transaction.end(

                                    true,

                                    object : OnObtain<Boolean?> {

                                        override fun onCompleted(data: Boolean?) {

                                            if (data != true) {

                                                val msg = "Failed to end obfuscation salt transaction"
                                                val e = Exception(msg)
                                                recordException(e)
                                            }
                                        }

                                        override fun onFailure(error: Throwable) {

                                            recordException(error)
                                        }
                                    }
                                )
                            }
                        }

                    } catch (e: Throwable) {

                        recordException(e)

                        result.error = e
                    }

                    if (isEmpty(result.takeValue())) {

                        result.firstTimeObtained.set(true)

                    } else {

                        result.updateValue()

                        result.firstTimeObtained.set(false)
                    }

                    callback.onCompleted(result)
                }

                obtain(

                    object : OnObtain<Secrets?> {

                        override fun onCompleted(data: Secrets?) = onSecrets(data)

                        override fun onFailure(error: Throwable) {

                            callback.onFailure(error)
                        }
                    }
                )

            } catch (e: Throwable) {

                callback.onFailure(e)
            }
        }
    }
}