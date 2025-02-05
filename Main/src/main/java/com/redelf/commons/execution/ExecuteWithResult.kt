package com.redelf.commons.execution

interface ExecuteWithResult<T> {

    fun execute(what: T): Boolean
}