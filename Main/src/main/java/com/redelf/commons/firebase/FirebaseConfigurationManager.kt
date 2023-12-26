package com.redelf.commons.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.defaults.ResourceDefaults
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    ContextualManager<FirebaseRemoteConfiguration>(),
    ResourceDefaults

{

    override val storageKey = "remote_configuration"

    override fun initializationCompleted(e: Exception?) {

        if (e == null) {

            val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
                .build()

            remoteConfig.setConfigSettingsAsync(configSettings)

            Timber.v("FirebaseConfigurationManager :: onInitialized")
        }

        super.initializationCompleted(e)
    }

    @Throws(IllegalArgumentException::class)
    override fun setDefaults(defaults: Int) {

        if (defaults <= 0) {

            throw IllegalArgumentException("Defaults must be a valid resource id")
        }

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(defaults)
    }
}