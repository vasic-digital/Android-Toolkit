package com.redelf.commons.delete

interface DeleteWithCallback<T> {

    fun delete(data: T, callback: DeleteCallback<T>)
}