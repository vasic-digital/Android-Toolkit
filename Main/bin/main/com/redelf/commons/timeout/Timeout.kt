package com.redelf.commons.timeout

interface Timeout {

    fun setTimeout(value: Int)

    fun getTimeout(): Int
}