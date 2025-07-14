package com.redelf.commons.management

import com.redelf.commons.obtain.OnObtain

interface DataPushListening {

    fun isRegisteredDataPushListener(subscriber: OnObtain<Boolean?>): Boolean

    fun registerDataPushListener(subscriber: OnObtain<Boolean?>)

    fun unregisterDataPushListener(subscriber: OnObtain<Boolean?>)
}