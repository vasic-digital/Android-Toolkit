package com.redelf.commons.filtering

import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.CopyOnWriteArraySet

interface FilterAsync<T> {

    fun filter(what: MutableList<T>, callback: OnObtain<Boolean?>)
}