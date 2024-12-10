package com.redelf.commons.referring.implementation.google

import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.loading.Loadable
import com.redelf.commons.loading.Unloadable
import com.redelf.commons.logging.Console
import com.redelf.commons.referring.InstallReferrer
import java.util.concurrent.atomic.AtomicBoolean

class GoogleInstallReferrer :

    Loadable, Unloadable,
    InstallReferrer<GoogleInstallReferrerData>()

{

    companion object {

        private var referrerData: GoogleInstallReferrerData? = null

        private const val keyGooglePlayInstantParam = "key.GooglePlayInstantParam"
        private const val keyInstallBeginTimestampSeconds = "key.InstallBeginTimestampSeconds"
        private const val keyReferrerClickTimestampSeconds = "key.ReferrerClickTimestampSeconds"
    }

    override val daysValid = 90
    override val tag = "${super.tag} Google ::"

    private val connected = AtomicBoolean()
    private var referrerClient: InstallReferrerClient? = null

    override fun getReferrerDataValue(): GoogleInstallReferrerData? {

        return referrerData
    }

    override fun setReferrerDataValue(value: GoogleInstallReferrerData?) {

        referrerData = value
    }

    override fun obtainReferrerData(): GoogleInstallReferrerData? {

        val referrerUrl: String = settings.getString(keyReferrerUrl, "")
        val referrerClickTime: Long = settings.getLong(keyReferrerClickTimestampSeconds, 0)
        val appInstallTime: Long = settings.getLong(keyInstallBeginTimestampSeconds, 0)
        val instantExperienceLaunched: Boolean = settings.getBoolean(keyGooglePlayInstantParam, false)

        val data = GoogleInstallReferrerData(

            referrerUrl = referrerUrl,
            installBeginTimestampSeconds = appInstallTime,
            referrerClickTimestampSeconds = referrerClickTime,
            googlePlayInstantParam = instantExperienceLaunched
        )

        setReferrerDataValue(data)

        return getReferrerDataValue()
    }

    override fun instantiateReferrerData(): GoogleInstallReferrerData? {

        load()

        referrerClient?.let { client ->

            val ref = client.installReferrer

            val referrerUrl: String = ref.installReferrer
            val referrerClickTime: Long = ref.referrerClickTimestampSeconds
            val appInstallTime: Long = ref.installBeginTimestampSeconds
            val instantExperienceLaunched: Boolean = ref.googlePlayInstantParam

            val data = GoogleInstallReferrerData(

                referrerUrl = referrerUrl,
                installBeginTimestampSeconds = appInstallTime,
                referrerClickTimestampSeconds = referrerClickTime,
                googlePlayInstantParam = instantExperienceLaunched
            )

            setReferrerDataValue(data)

            settings.putString(keyReferrerUrl, data.referrerUrl ?: "")
            settings.putBoolean(keyGooglePlayInstantParam, data.googlePlayInstantParam ?: false)
            settings.putLong(keyInstallBeginTimestampSeconds, data.installBeginTimestampSeconds ?: 0)
            settings.putLong(keyReferrerClickTimestampSeconds, data.referrerClickTimestampSeconds ?: 0)

            unload()
        }

        return getReferrerDataValue()
    }

    override fun load() {

        val tag = "$tag Load ::"

        Console.log("$tag START")

        if (isOnMainThread()) {

            val e = IllegalStateException("RUNNING ON MAIN THREAD :: ${this::class.simpleName}.load")
            recordException(e)
        }

        if (connected.get()) {

            Console.log("$tag END :: Already loaded")
            return
        }

        referrerClient = InstallReferrerClient.newBuilder(takeContext()).build()

        referrerClient?.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {

                when (responseCode) {

                    InstallReferrerResponse.OK -> {

                        connected.set(true)

                        Console.log("$tag END")
                    }

                    InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {

                        Console.error("$tag ERROR: Not supported")
                    }

                    InstallReferrerResponse.SERVICE_UNAVAILABLE -> {

                        Console.error("$tag ERROR: Not available")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {

                connected.set(true)
            }
        })
    }

    override fun unload() {

        val tag = "$tag UNLOAD ::"

        Console.log("$tag START")

        if (isOnMainThread()) {

            val e = IllegalStateException("RUNNING ON MAIN THREAD :: ${this::class.simpleName}.unload")
            recordException(e)
        }

        try {

            referrerClient?.endConnection()
            referrerClient = null

            Console.log("$tag END")

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }
    }

    override fun isLoaded() = connected.get()
}