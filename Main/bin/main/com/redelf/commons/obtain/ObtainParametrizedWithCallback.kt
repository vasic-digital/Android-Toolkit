package com.redelf.commons.obtain

interface ObtainParametrizedWithCallback<T, P> {

    fun obtain(param: P, callback: OnObtain<T>)
}