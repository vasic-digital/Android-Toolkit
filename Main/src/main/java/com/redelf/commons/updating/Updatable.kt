package com.redelf.commons.updating

import com.redelf.commons.obtain.OnObtain

interface Updatable<T> {

    fun update()

    fun isUpdating(): Boolean

    fun update(identifier: T): Boolean

    fun onUpdated(identifier: T)

    fun onUpdatedFailed(identifier: T)

    fun isUpdateApplied(identifier: T, callback: OnObtain<Boolean>)
}