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
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    Loadable,
    ResourceDefaults,
    ContextualManager<ConcurrentHashMap<String, FirebaseRemoteConfigValue>>() {

    override val persist = false
    override val storageKey = "remote_configuration"

    private const val LOG_TAG = "FirebaseConfigurationManager ::"

    override fun getLogTag() = "$LOG_TAG ${hashCode()} ::"

    override fun getWho(): String? = FirebaseConfigurationManager::class.java.simpleName

    private val loaded = AtomicBoolean()

    override fun isLazyReady() = loaded.get()

    override fun isLoaded() = isLazyReady()

    override fun reset(): Boolean {

        loaded.set(false)

        return super.reset()
    }

    override fun load() {

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        Console.log("$LOG_TAG Config params fetching")

        val latch = CountDownLatch(1)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val updated = task.result
                    val msg = "$LOG_TAG Config params updated: $updated"

                    if (updated) {

                        Console.debug(msg)

                    } else {

                        Console.log(msg)
                    }

                    val all = remoteConfig.all

                    Console.log("$LOG_TAG Config params obtained: $all")

                    try {

                        val newMap = ConcurrentHashMap<String, FirebaseRemoteConfigValue>()
                        newMap.putAll(all)
                        pushData(newMap)

                    } catch (e: IllegalStateException) {

                        recordException(e)
                    }

                    latch.countDown()

                } else {

                    Console.error("$LOG_TAG Config params update failed")
                }

                loaded.set(true)

            }.addOnFailureListener {

                loaded.set(true)

                Console.error("$LOG_TAG Config params update failed (2)")

                latch.countDown()
            }

        try {

            if (latch.await(60, TimeUnit.SECONDS)) {

                Console.log("$LOG_TAG SUCCESS")

            } else {

                val e = TimeoutException("Timeout-ed while waiting for firebase manager to load data")

                recordException(e)

                loaded.set(true)
            }

        } catch (e: Exception) {

            loaded.set(true)

            recordException(e)
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