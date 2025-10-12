package com.redelf.commons.filtering

interface Filter<T> {

    fun filter(what: MutableList<T>): FilterResult<T>
}