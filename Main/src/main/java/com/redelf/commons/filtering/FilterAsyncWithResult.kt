package com.redelf.commons.filtering

import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.CopyOnWriteArraySet

interface FilterAsyncWithResult<T> {

    fun filter(what: CopyOnWriteArraySet<T>, callback: OnObtain<FilterResult<T>?>)
}