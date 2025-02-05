package com.redelf.commons.referring

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.expiration.ExpirationParametrized
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.settings.SettingsManagement
import com.redelf.commons.settings.SettingsManager

abstract class InstallReferrer<T>(

    protected val settings: SettingsManagement = SettingsManager.obtain()

) :

    Obtain<T?>,
    ContextAvailability<BaseApplication> where T : ExpirationParametrized<Int>

{

    protected abstract val daysValid: Int

    protected val keyVersionCode = "key.VersionCode"
    protected val keyReferrerUrl = "key.ReferrerUrl"

    protected open val tag = "Install referrer ::"

    override fun takeContext(): BaseApplication {

        return BaseApplication.takeContext()
    }

    override fun obtain(): T? {

        val tag = "$tag OBTAIN ::"

        Console.log("$tag START")

        if (isOnMainThread()) {

            val e = IllegalStateException(

                "RUNNING ON MAIN THREAD :: ${this::class.simpleName}.obtain"
            )

            recordException(e)
        }

        try {

            val versionCode = BaseApplication.getVersionCode()
            val existingVersionCode = settings.getString(keyVersionCode, "")

            if (versionCode != existingVersionCode) {

                settings.putString(keyVersionCode, versionCode)

                Console.log(

                    "$tag INSTANTIATE :: " +
                            "Version code changed from $existingVersionCode to $versionCode"
                )

                instantiateReferrerData()

            } else {

                val dataValue = getReferrerDataValue()

                if (dataValue == null) {

                    Console.log("$tag NO REFERRER DATA LOADED")

                    val data = obtainReferrerData()

                    Console.log("$tag LOAD :: START")

                    data?.let {

                        Console.log(

                            "$tag LOAD :: END :: Referrer data available " +
                                    "after loading from settings"
                        )

                        if (it.isExpired(daysValid)) {

                            Console.log("$tag INSTANTIATE :: Data expired (1)")

                            instantiateReferrerData()
                        }
                    }

                    if (data == null) {

                        Console.log(

                            "$tag INSTANTIATE :: No referrer data available " +
                                    "after loading from settings"
                        )

                        instantiateReferrerData()
                    }

                } else {

                    Console.log("$tag REFERRER DATA ALREADY LOADED")

                    if (dataValue.isExpired(daysValid)) {

                        Console.log("$tag INSTANTIATE :: Data expired (2)")

                        instantiateReferrerData()
                    }
                }
            }

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }

        val result = getReferrerDataValue()

        Console.log("$tag END: $result")

        return result
    }

    protected abstract fun obtainReferrerData(): T?

    protected abstract fun instantiateReferrerData(): T?

    protected abstract fun getReferrerDataValue(): T?

    protected abstract fun setReferrerDataValue(value: T?)
}