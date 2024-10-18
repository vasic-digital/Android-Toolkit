package com.redelf.commons.stateful

interface Stateful {

    fun onStateChanged()

    fun onState(state: State)

    fun getState(): State

    fun setState(state: State)
}