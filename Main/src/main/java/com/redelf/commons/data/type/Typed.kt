package com.redelf.commons.data.type


fun interface Typed<T> {

    fun getClazz(): Class<*>
}