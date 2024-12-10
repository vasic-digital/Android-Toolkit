package com.redelf.commons.referring

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.settings.SettingsManagement
import com.redelf.commons.settings.SettingsManager

abstract class InstallReferrerDataManager<T>(

    protected val settings: SettingsManagement = SettingsManager.obtain()

) :

    Obtain<T?>,
    ContextAvailability<BaseApplication>

{

    protected open val tag = "Install referrer ::"

    override fun takeContext(): BaseApplication {

        return BaseApplication.takeContext()
    }
}