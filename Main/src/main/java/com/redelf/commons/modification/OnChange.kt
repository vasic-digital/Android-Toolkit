package com.redelf.commons.modification

interface OnChange : OnChangeStarted {

    fun onChange(notify: Boolean, action: String): Boolean
}