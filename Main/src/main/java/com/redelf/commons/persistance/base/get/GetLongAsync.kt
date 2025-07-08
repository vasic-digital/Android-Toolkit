package com.redelf.commons.persistance.base.get

import com.redelf.commons.obtain.OnObtain

interface GetLongAsync {

    fun getLong(key: String, defaultValue: Long, callback: OnObtain<Long>)
}