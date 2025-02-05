package com.redelf.commons.destruction.delete

interface DeleteCallback<T> {

    fun onDelete(data: T)

    fun onFailure(error: Throwable)
}