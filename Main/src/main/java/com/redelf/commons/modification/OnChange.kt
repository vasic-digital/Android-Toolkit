package com.redelf.commons.modification

interface OnChange : OnChangeStarted {

    fun onChange(action: String): Boolean
}