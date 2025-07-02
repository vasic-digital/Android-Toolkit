package com.redelf.commons.direction

import com.redelf.commons.obtain.OnObtain

interface PullAsync<K> {

    fun <T> pull(key: K, callback: OnObtain<T?>)
}