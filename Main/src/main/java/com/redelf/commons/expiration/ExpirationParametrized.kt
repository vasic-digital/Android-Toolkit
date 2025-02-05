package com.redelf.commons.expiration

interface ExpirationParametrized<T> {

    fun isExpired(beginning: T): Boolean
}