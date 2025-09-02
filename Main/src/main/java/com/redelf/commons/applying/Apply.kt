package com.redelf.commons.applying

import com.redelf.commons.obtain.OnObtain

interface Apply<T, R> :

    ApplyAndNotify,
    ApplyAndNotifyData<T>,
    ApplyAndNotifyDataWithCallback<T, R>

{

    fun apply(from: String): Boolean
}