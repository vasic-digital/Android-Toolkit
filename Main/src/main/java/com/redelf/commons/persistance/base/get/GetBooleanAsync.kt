package com.redelf.commons.persistance.base.get

import com.redelf.commons.obtain.OnObtain

interface GetBooleanAsync {

    fun getBoolean(key: String, defaultValue: Boolean, callback: OnObtain<Boolean>)
}