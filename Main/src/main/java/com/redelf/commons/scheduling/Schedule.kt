package com.redelf.commons.scheduling

interface Schedule<T> {

    fun schedule(what: T, toWhen: Long): Boolean

    fun unSchedule(what: T): Boolean
}