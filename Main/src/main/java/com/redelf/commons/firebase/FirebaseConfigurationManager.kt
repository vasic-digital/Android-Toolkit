package com.redelf.commons.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.redelf.commons.context.ContextualManager

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager : ContextualManager<FirebaseRemoteConfiguration>() {

    override val storageKey = "remote_configuration"

    override fun onInitialized(e: Exception?) {
        super.onInitialized(e)

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
    }
}