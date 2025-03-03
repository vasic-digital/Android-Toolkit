package com.redelf.commons.referring.implementation.facebook

import com.facebook.applinks.AppLinkData
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.referring.InstallReferrer

class FacebookInstallReferrer : InstallReferrer<FacebookInstallReferrerData>() {

    companion object {

        private const val keyMir = "key.Mir"

        private var referrerData: FacebookInstallReferrerData? = null
    }

    override val daysValid = 28
    override val tag = "${super.tag} Facebook ::"

    override fun getReferrerDataValue(): FacebookInstallReferrerData? {

        return referrerData
    }

    override fun setReferrerDataValue(value: FacebookInstallReferrerData?) {

        referrerData = value
    }

    override fun obtainReferrerData(): FacebookInstallReferrerData? {

        val mir: String = settings.getString(keyMir, "")
        val data = FacebookInstallReferrerData(mir)

        setReferrerDataValue(data)

        return getReferrerDataValue()
    }

    override fun instantiateReferrerData(): FacebookInstallReferrerData? {

        try {

            val intent = takeContext().takeIntent()
            val appLinkData = AppLinkData.createFromAlApplinkData(intent)
            val mir = appLinkData?.targetUri?.getQueryParameter("fbclid")

            mir?.let {

                if (isNotEmpty(it)) {

                    return FacebookInstallReferrerData(it)
                }
            }

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }
}