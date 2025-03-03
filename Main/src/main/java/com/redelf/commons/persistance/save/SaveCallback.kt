package com.redelf.commons.persistance.save

interface SaveCallback<T> {

    fun onSave(data: T)

    fun onFailure(error: Throwable)
}