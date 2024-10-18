package com.redelf.commons.stateful

interface Stateful<T> {

    fun onStateChanged()

    fun onState(state: State<T>)

    fun getState(): State<T>

    fun setState(state: State<T>)
}