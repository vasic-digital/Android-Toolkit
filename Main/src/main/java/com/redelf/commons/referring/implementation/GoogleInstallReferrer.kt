package com.redelf.commons.referring.implementation

import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.referring.InstallReferrerDataManager
import java.util.concurrent.atomic.AtomicBoolean

class GoogleInstallReferrer : InstallReferrerDataManager<ReferrerDetails>() {

    companion object {

        private const val keyVersionCode = "KEY.VERSION_CODE"
    }

    override val tag = "${super.tag} Google ::"

    private val connected = AtomicBoolean()
    private lateinit var referrerClient: InstallReferrerClient

    override fun load() {

        val tag = "$tag Load ::"

        Console.log("$tag START")

        if (connected.get()) {

            Console.log("$tag END :: Already loaded")
            return
        }

        referrerClient = InstallReferrerClient.newBuilder(takeContext()).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {

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

    override fun get(key: String, defaultValue: ReferrerDetails): ReferrerDetails? {

        val tag = "$tag GET ::"

        Console.log("$tag START")

        var details: ReferrerDetails? = null

        try {

            val versionCode = BaseApplication.getVersionCode()
            val existingVersionCode = settings.getString(keyVersionCode, "")

            if (versionCode != existingVersionCode) {

                settings.putString(keyVersionCode, versionCode)

            } else {


            }

//            withLoadable()

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }

        return details
    }
}