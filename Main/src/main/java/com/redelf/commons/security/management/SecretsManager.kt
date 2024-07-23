package com.redelf.commons.security.management

import android.annotation.SuppressLint
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.data.type.Typed
import com.redelf.commons.logging.Console

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
}