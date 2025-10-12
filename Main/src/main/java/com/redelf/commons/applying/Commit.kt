package com.redelf.commons.applying

interface Commit : Committable {

    fun commit(from: String): Boolean
}