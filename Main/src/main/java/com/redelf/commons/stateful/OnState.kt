package com.redelf.commons.stateful

interface OnState<T> {

    fun onStateChanged(whoseState: Class<*>? = null)

    fun onState(state: State<T>, whoseState: Class<*>? = null)
}