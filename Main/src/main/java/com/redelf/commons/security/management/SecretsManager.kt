package com.redelf.commons.security.management

import android.annotation.SuppressLint
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.data.type.Typed
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.security.obfuscation.ObfuscatorSaltObtain
import com.redelf.commons.security.obfuscation.RemoteObfuscatorSaltObtain

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

    override val storageKey = "s3_cR3_tZ" +
            "${BaseApplication.getVersion()}_${BaseApplication.getVersionCode()}"

    override fun getLogTag() = "SecretsManager :: ${hashCode()} ::"

    override fun createDataObject() = Secrets()

    fun setObfuscationSalt(source: RemoteObfuscatorSaltObtain) {

        exec(

            onRejected = { err -> recordException(err) }

        ) {

            val transaction = transaction("setObfuscationSalt")

            try {

                val data = obtain()

                data?.let {

                    val newSalt = source.getRemoteData()

                    if (isNotEmpty(newSalt)) {

                        it.obfuscationSalt = newSalt

                        transaction.end()
                    }
                }

            } catch (e: Exception) {

                recordException(e)
            }
        }
    }
}