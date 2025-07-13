package com.redelf.commons.filtering

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


data class FilterResult<T> (

    val filteredItems: MutableList<T>,
    val wasModified: Boolean,
    val changedCount: AtomicInteger = AtomicInteger()

) {

    private val modified: AtomicBoolean = AtomicBoolean(wasModified)

    fun isModified() = modified.get() || changedCount.get() > 0
}
