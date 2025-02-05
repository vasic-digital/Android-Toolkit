package com.redelf.commons.stateful

interface SetState<T> {

    fun setState(state: State<T>)
}