package com.redelf.commons.creation.instantiation

import com.redelf.commons.obtain.ObtainParametrized

abstract class MultiInstanceParametrized<T> : Instantiable<T>, ObtainParametrized<T, Any> {

    @Throws(IllegalArgumentException::class)
    override fun obtain(param: Any): T = instantiate(param)
}