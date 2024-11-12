package com.redelf.commons.lifecycle

interface Initialization : InitializationCondition {

    fun initialize(): Boolean
}