package com.redelf.commons.delete

interface DeleteCallback<T> {

    fun onDelete(data: T)

    fun onFailure(error: Throwable)
}