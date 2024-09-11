package com.redelf.commons.processing

interface Process<in WHAT> {

    fun process(input: WHAT)
}