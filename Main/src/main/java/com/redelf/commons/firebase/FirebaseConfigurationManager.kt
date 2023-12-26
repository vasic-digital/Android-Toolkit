package com.redelf.commons.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.defaults.Defaults
import com.redelf.commons.defaults.ResourceDefaults
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    ContextualManager<FirebaseRemoteConfiguration>(),
    ResourceDefaults

{

    override val storageKey = "remote_configuration"

    override fun onInitialized(e: Exception?) {
        super.onInitialized(e)

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        Timber.v("FirebaseConfigurationManager :: onInitialized")
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