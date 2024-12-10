package com.redelf.commons.referring.implementation.facebook

import com.facebook.applinks.AppLinkData
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.referring.InstallReferrerDataManager

class FacebookInstallReferrer : InstallReferrerDataManager<FacebookInstallReferrerData>() {

    override val tag = "${super.tag} Facebook ::"

    override fun obtain(): FacebookInstallReferrerData? {

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