package com.redelf.commons.registration

interface Unregister<T> {

    fun unregister(subscriber: T)
}