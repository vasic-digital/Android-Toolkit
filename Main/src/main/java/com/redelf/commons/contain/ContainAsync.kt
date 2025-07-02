package com.redelf.commons.contain

import com.redelf.commons.obtain.OnObtain

interface ContainAsync<K> {

    fun contains(key: K, callback: OnObtain<Boolean>)
}