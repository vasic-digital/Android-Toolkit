package com.redelf.commons.modification

interface OnChangeCompleted {

    fun onChange(action: String, changed: Boolean)
}