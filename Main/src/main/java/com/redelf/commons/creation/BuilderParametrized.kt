package com.redelf.commons.creation

interface BuilderParametrized<IN, OUT> : Building {

    fun build(input: IN): OUT
}