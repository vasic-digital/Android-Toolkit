package com.redelf.commons.referring.implementation.google

import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.loading.Loadable
import com.redelf.commons.loading.Unloadable
import com.redelf.commons.logging.Console
import com.redelf.commons.referring.InstallReferrerDataManager
import java.util.concurrent.atomic.AtomicBoolean

class GoogleInstallReferrer :

    Loadable, Unloadable,
    InstallReferrerDataManager<GoogleInstallReferrerData>()

{

    companion object {

        private var details: GoogleInstallReferrerData? = null

        private const val keyVersionCode = "key.VersionCode"
        private const val keyReferrerUrl = "key.ReferrerUrl"
        private const val keyGooglePlayInstantParam = "key.GooglePlayInstantParam"
        private const val keyInstallBeginTimestampSeconds = "key.InstallBeginTimestampSeconds"
        private const val keyReferrerClickTimestampSeconds = "key.ReferrerClickTimestampSeconds"
    }

    override val tag = "${super.tag} Google ::"

    private val connected = AtomicBoolean()
    private var referrerClient: InstallReferrerClient? = null

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

    override fun isLoaded() = connected.get()

    override fun obtain(): GoogleInstallReferrerData? {

        val tag = "$tag GET ::"

        Console.log("$tag START")

        if (isOnMainThread()) {

            val e = IllegalStateException("RUNNING ON MAIN THREAD :: ${this::class.simpleName}.obtain")
            recordException(e)
        }

        try {

            val versionCode = BaseApplication.getVersionCode()
            val existingVersionCode = settings.getString(keyVersionCode, "")

            if (versionCode != existingVersionCode) {

                load()

                settings.putString(keyVersionCode, versionCode)

                referrerClient?.let { client ->

                    val ref = client.installReferrer

                    val referrerUrl: String = ref.installReferrer
                    val referrerClickTime: Long = ref.referrerClickTimestampSeconds
                    val appInstallTime: Long = ref.installBeginTimestampSeconds
                    val instantExperienceLaunched: Boolean = ref.googlePlayInstantParam

                    details = GoogleInstallReferrerData(

                        referrerUrl = referrerUrl,
                        installBeginTimestampSeconds = appInstallTime,
                        referrerClickTimestampSeconds = referrerClickTime,
                        googlePlayInstantParam = instantExperienceLaunched
                    )

                    settings.putString(keyReferrerUrl, details?.referrerUrl ?: "")
                    settings.putBoolean(keyGooglePlayInstantParam, details?.googlePlayInstantParam ?: false)
                    settings.putLong(keyInstallBeginTimestampSeconds, details?.installBeginTimestampSeconds ?: 0)
                    settings.putLong(keyReferrerClickTimestampSeconds, details?.referrerClickTimestampSeconds ?: 0)

                    unload()
                }

            } else {

                if (details == null) {

                    val referrerUrl: String = settings.getString(keyReferrerUrl, "")
                    val referrerClickTime: Long = settings.getLong(keyReferrerClickTimestampSeconds, 0)
                    val appInstallTime: Long = settings.getLong(keyInstallBeginTimestampSeconds, 0)
                    val instantExperienceLaunched: Boolean = settings.getBoolean(keyGooglePlayInstantParam, false)

                    details = GoogleInstallReferrerData(

                        referrerUrl = referrerUrl,
                        installBeginTimestampSeconds = appInstallTime,
                        referrerClickTimestampSeconds = referrerClickTime,
                        googlePlayInstantParam = instantExperienceLaunched
                    )
                }
            }

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }

        return details
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
}