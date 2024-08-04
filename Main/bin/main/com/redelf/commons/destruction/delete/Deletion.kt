package com.redelf.commons.destruction.delete

interface Deletion<T> {

    fun delete(what: T): Boolean
}