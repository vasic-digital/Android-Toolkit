package com.redelf.commons.updating

interface Updatable<T> {

    fun update()

    fun isUpdating(): Boolean

    fun update(identifier: T): Boolean

    fun onUpdated(identifier: T)

    fun onUpdatedFailed(identifier: T)

    fun isUpdateApplied(identifier: T): Boolean
}