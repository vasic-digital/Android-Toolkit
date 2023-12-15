package com.redelf.commons.save

interface SaveCallback<T> {

    fun onSave(data: T)

    fun onFailure(error: Throwable)
}