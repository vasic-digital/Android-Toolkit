package com.redelf.commons.management

import com.redelf.commons.obtain.OnObtain

interface DataPushListening {

    fun isRegisteredDataPushListener(subscriber: OnObtain<DataPushResult?>): Boolean

    fun registerDataPushListener(subscriber: OnObtain<DataPushResult?>)

    fun unregisterDataPushListener(subscriber: OnObtain<DataPushResult?>)
}