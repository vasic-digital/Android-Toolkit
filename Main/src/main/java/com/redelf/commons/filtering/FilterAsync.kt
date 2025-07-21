package com.redelf.commons.filtering

import com.redelf.commons.obtain.OnObtain

interface FilterAsync<T> {

    fun filter(what: MutableList<T>, callback: OnObtain<FilterResult<T>?>)
}