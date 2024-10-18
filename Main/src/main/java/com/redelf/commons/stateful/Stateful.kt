package com.redelf.commons.stateful

interface Stateful {

    fun onState(state: State)

    fun getState(state: State)

    fun setState(state: State)
}