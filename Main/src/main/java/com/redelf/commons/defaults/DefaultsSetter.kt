package com.redelf.commons.defaults

interface DefaultsSetter<in T> {

    fun setDefaults(defaults: T)
}