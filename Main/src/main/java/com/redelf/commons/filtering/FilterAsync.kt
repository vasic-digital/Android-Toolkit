package com.redelf.commons.filtering

import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.CopyOnWriteArraySet

interface FilterAsync<T> {

    fun filter(from: String, what: CopyOnWriteArraySet<T>, callback: OnObtain<Boolean?>)
}