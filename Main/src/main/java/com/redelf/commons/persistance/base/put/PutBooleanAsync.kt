package com.redelf.commons.persistance.base.put

import com.redelf.commons.obtain.OnObtain

interface PutBooleanAsync {

    fun putBoolean(key: String, value: Boolean, callback: OnObtain<Boolean>)
}