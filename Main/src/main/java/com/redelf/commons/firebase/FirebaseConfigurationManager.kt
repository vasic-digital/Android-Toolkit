package com.redelf.commons.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.recordException
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    ContextualManager<Map<String, FirebaseRemoteConfigValue>>(),
    ResourceDefaults {

    override val storageKey = "remote_configuration"

    private const val logTag = "FirebaseConfigurationManager ::"

    override fun getWho(): String? = FirebaseConfigurationManager::class.java.simpleName

    @Throws(IllegalStateException::class)
    override fun initialization(): Boolean {

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        Timber.v("$logTag Config params fetching")

        val latch = CountDownLatch(1)
        val success = AtomicBoolean(false)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val updated = task.result
                    val msg = "$logTag Config params updated: $updated"

                    if (updated) {

                        Timber.d(msg)

                    } else {

                        Timber.v(msg)
                    }

                    val all = remoteConfig.all

                    Timber.v("$logTag Config params obtained: $all")

                    try {

                        pushData(all)
                        success.set(true)

                    } catch (e: IllegalStateException) {

                        recordException(e)
                    }

                    latch.countDown()

                } else {

                    Timber.e("$logTag Config params update failed")
                }
            }

        latch.await()

        return success.get()
    }

    override fun initializationCompleted(e: Exception?) {

        e?.let {

            Timber.e(e)
        }
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