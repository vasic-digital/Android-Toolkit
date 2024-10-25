package com.redelf.commons.stateful

interface OnState<T> {

    fun onStateChanged()

    fun onState(state: State<T>)
}