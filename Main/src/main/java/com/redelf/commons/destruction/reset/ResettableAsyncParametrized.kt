package com.redelf.commons.destruction.reset

import com.redelf.commons.obtain.OnObtain

interface ResettableAsyncParametrized<T> : Resetting {

    fun reset(arg: T, callback: OnObtain<Boolean?>)
}