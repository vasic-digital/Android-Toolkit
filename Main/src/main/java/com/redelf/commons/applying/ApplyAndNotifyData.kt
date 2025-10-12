package com.redelf.commons.applying

interface ApplyAndNotifyData<T> : Applying {

    fun apply(from: String, data: T?, notify: Boolean): Boolean
}