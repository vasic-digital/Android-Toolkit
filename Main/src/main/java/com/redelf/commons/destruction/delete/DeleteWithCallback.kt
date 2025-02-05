package com.redelf.commons.destruction.delete

interface DeleteWithCallback<T> {

    fun delete(data: T, callback: DeleteCallback<T>)
}