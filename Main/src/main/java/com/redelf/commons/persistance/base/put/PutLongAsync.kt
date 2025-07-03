package com.redelf.commons.persistance.base.put

import com.redelf.commons.obtain.OnObtain

interface PutLongAsync {

    fun putLong(key: String, value: Long, callback: OnObtain<Boolean>)
}