package com.redelf.commons.applying

import com.redelf.commons.obtain.OnObtain

interface ApplyAndNotifyDataWithCallback<T, R> : Applying {

    fun apply(data: T?, from: String, notify: Boolean, callback: OnObtain<R?>?)
}