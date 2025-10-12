package com.redelf.commons.lifecycle.initialization

interface Initialization : InitializationCondition {

    fun initialize(): Boolean
}