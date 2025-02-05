package com.redelf.commons.obtain

import com.redelf.commons.obtain.suspendable.Obtain

interface Obtainer<T> {

    fun getObtainer(vararg params: Any): Obtain<T>
}