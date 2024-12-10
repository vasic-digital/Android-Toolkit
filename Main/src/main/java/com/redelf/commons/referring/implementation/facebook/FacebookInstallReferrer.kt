package com.redelf.commons.referring.implementation.facebook

import com.facebook.applinks.AppLinkData
import com.redelf.commons.referring.InstallReferrerDataManager

class FacebookInstallReferrer : InstallReferrerDataManager<FacebookInstallReferrerData>() {

    override val tag = "${super.tag} Facebook ::"

    override fun obtain(): FacebookInstallReferrerData? {

        // Get the App Link Data
        val appLinkData = AppLinkData.createFromAlApplinkData(intent.data)

// Get the Meta Install Referrer (MIR)
        val mir = appLinkData.targetUri.getQueryParameter("fbclid")
    }
}