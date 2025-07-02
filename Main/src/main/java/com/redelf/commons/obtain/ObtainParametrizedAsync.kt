package com.redelf.commons.obtain

interface ObtainParametrizedAsync<T, P> {

    fun obtain(param: P, callback: OnObtain<T>)
}