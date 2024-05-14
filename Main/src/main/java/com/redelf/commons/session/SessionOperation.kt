package com.redelf.commons.session

interface SessionOperation {

    fun start(): Boolean

    fun perform(): Boolean

    fun end(): Boolean
}