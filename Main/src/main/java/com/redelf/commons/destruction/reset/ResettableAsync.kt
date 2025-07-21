package com.redelf.commons.destruction.reset

import com.redelf.commons.obtain.OnObtain

interface ResettableAsync : Resetting {

    fun reset(callback: OnObtain<Boolean?>)
}