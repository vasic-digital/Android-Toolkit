package com.redelf.commons.applying

interface Apply<T, R> :

    ApplyAndNotify,
    ApplyAndNotifyData<T>,
    ApplyAndNotifyDataWithCallback<T, R>

{

    fun apply(from: String): Boolean
}