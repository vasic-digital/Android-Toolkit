package com.redelf.commons.persistance.base.put

import com.redelf.commons.obtain.OnObtain

interface PutAsync<T> {

    fun put(key: String, value: T, callback: OnObtain<Boolean>)
}