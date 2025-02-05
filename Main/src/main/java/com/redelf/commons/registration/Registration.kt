package com.redelf.commons.registration

interface Registration<T> : Register<T>, Unregister<T> {

    fun isRegistered(subscriber: T): Boolean
}