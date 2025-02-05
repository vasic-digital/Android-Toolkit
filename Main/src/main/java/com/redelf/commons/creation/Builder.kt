package com.redelf.commons.creation

interface Builder<T> : Building {

    fun build(): T
}