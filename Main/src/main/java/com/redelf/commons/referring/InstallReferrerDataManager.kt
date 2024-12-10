package com.redelf.commons.referring

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.settings.SettingsManagement
import com.redelf.commons.settings.SettingsManager

abstract class InstallReferrerDataManager<T>(

    protected val settings: SettingsManagement = SettingsManager.obtain()

) :

    Obtain<T?>,
    ContextAvailability<BaseApplication>

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

                instantiateReferrerData()

            } else {

                if (getReferrerDataValue() == null) {

                    obtainReferrerData()
                }
            }

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")
            recordException(e)
        }

        return getReferrerDataValue()
    }

    protected abstract fun obtainReferrerData(): T?

    protected abstract fun instantiateReferrerData(): T?

    protected abstract fun getReferrerDataValue(): T?

    protected abstract fun setReferrerDataValue(value: T?)
}