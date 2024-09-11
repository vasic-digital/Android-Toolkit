package com.redelf.commons.messaging.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.extensions.recordException
import com.redelf.commons.loading.Loadable
import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    ContextualManager<ConcurrentHashMap<String, FirebaseRemoteConfigValue>>(),
    ResourceDefaults,
    Loadable

{

    override val persist = false
    override val storageKey = "remote_configuration"

    private const val logTag = "FirebaseConfigurationManager ::"

    override fun getLogTag() = "$logTag ${hashCode()} ::"

    override fun getWho(): String? = FirebaseConfigurationManager::class.java.simpleName

    override fun load() {

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        Console.log("$logTag Config params fetching")

        val latch = CountDownLatch(1)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val updated = task.result
                    val msg = "$logTag Config params updated: $updated"

                    if (updated) {

                        Console.debug(msg)

                    } else {

                        Console.log(msg)
                    }

                    val all = remoteConfig.all

                    Console.log("$logTag Config params obtained: $all")

                    try {

                        val newMap = ConcurrentHashMap<String, FirebaseRemoteConfigValue>()
                        newMap.putAll(all)
                        pushData(newMap)

                    } catch (e: IllegalStateException) {

                        recordException(e)
                    }

                    latch.countDown()

                } else {

                    Console.error("$logTag Config params update failed")
                }
            }

        latch.await()
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