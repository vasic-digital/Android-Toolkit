package com.redelf.commons.destruction.clear

interface ClearingFromAndNotify : Clear {

    fun clear(from: String, notify: Boolean)
}