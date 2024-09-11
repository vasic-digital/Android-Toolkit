package com.redelf.commons.registration

interface Register<T> {

    fun register(subscriber: T)
}