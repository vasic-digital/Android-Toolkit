package com.redelf.commons.destruction.delete

interface DeletionCheck {

    fun isDeleted(): Boolean

    fun isNotDeleted() = !isDeleted()
}