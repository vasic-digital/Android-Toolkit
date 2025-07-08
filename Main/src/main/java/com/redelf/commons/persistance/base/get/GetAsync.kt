package com.redelf.commons.persistance.base.get

import com.redelf.commons.obtain.OnObtain

interface GetAsync<T> {

    fun get(key: String, defaultValue: T, callback: OnObtain<T?>)
}