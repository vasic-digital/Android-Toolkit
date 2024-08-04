package com.redelf.commons.session

interface SessionOperation {

    fun start(): Boolean = true

    fun perform(): Boolean

    fun end(): Boolean
}