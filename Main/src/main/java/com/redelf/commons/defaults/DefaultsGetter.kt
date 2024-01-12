package com.redelf.commons.defaults

interface DefaultsGetter<out T> {

    fun getDefaults(): T?
}