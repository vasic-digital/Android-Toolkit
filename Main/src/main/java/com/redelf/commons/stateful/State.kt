package com.redelf.commons.stateful

interface State<T> {

    fun getState(): T
}