package com.redelf.commons.destruction.delete

import com.redelf.commons.obtain.OnObtain

interface DeletionAsync<T> {

    fun delete(what: T, callback: OnObtain<Boolean?>)
}