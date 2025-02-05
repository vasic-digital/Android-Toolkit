package com.redelf.commons.contain

interface Contain<K> {

    fun contains(key: K): Boolean
}