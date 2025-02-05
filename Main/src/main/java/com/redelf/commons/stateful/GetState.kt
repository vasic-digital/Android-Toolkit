package com.redelf.commons.stateful

interface GetState<T> {

    fun getState(): State<T>
}