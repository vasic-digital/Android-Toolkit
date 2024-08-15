package com.redelf.commons.processing

interface Process<in WHAT, out RESULT> {

    fun process(input: WHAT): RESULT
}