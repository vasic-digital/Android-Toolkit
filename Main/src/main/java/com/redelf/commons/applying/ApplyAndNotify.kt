package com.redelf.commons.applying

interface ApplyAndNotify : Applying {

    fun apply(from: String, notify: Boolean): Boolean
}