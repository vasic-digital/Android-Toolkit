package com.redelf.commons.persistance.base.put

import com.redelf.commons.obtain.OnObtain

interface PutStringAsync {

    fun putString(key: String, value: String, callback: OnObtain<Boolean>)
}