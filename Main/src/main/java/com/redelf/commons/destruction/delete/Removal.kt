package com.redelf.commons.destruction.delete

interface Removal<T> : Deletion<T> {

    fun remove(what: T) = delete(what)
}