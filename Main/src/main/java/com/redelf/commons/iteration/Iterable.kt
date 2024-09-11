package com.redelf.commons.iteration

interface Iterable<T> {

    fun getIterator(): MutableIterator<T>
}