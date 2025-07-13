package com.redelf.commons.filtering

import java.util.LinkedList

interface Filter<T> {

    fun filter(what: MutableList<T>): FilterResult<T>
}