package com.redelf.commons.referring.implementation.facebook

import com.facebook.applinks.AppLinkData
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.referring.InstallReferrerDataManager
import com.redelf.commons.referring.implementation.google.GoogleInstallReferrer
import com.redelf.commons.referring.implementation.google.GoogleInstallReferrerData

class FacebookInstallReferrer : InstallReferrerDataManager<FacebookInstallReferrerData>() {

    companion object {

        private const val keyMir = "key.Mir"

        private var referrerData: FacebookInstallReferrerData? = null
    }

    override val daysValid = 28
    override val tag = "${super.tag} Facebook ::"

    override fun obtainReferrerData(): FacebookInstallReferrerData? {

        TODO("Not yet implemented")
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

    override fun getReferrerDataValue(): FacebookInstallReferrerData? {

        return referrerData
    }

    override fun setReferrerDataValue(value: FacebookInstallReferrerData?) {

        referrerData = value
    }
}