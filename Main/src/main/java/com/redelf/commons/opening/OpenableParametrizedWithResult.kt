package com.redelf.commons.opening

interface OpenableParametrizedWithResult<P, T> {

    fun open(param: P?): T
}