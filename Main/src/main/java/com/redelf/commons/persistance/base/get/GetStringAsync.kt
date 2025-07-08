package com.redelf.commons.persistance.base.get

import com.redelf.commons.obtain.OnObtain

interface GetStringAsync {

    fun getString(key: String, defaultValue: String, callback: OnObtain<String>)
}