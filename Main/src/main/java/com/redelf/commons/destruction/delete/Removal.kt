package com.redelf.commons.destruction.delete

interface Removal<T> {

    fun remove(what: T): Boolean
}