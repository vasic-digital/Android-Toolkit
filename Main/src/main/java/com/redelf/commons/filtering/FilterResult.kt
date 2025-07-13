package com.redelf.commons.filtering

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


data class FilterResult<T> (

    val filteredItems: MutableList<T>,
    val modified: AtomicBoolean = AtomicBoolean(),
    val changedCount: AtomicInteger = AtomicInteger()

) {

    fun isModified() = modified.get() || changedCount.get() > 0
}
