package com.redelf.commons.dependency

interface Chainable<T> {

    fun chain(what: T) : Chainable<T>

    fun unchain(what: T) : Chainable<T>
}