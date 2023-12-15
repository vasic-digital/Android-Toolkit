package com.redelf.commons.delete

interface Deletion<T> {

    fun delete(what: T): Boolean
}