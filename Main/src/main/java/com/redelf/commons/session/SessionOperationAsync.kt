package com.redelf.commons.session

import com.redelf.commons.obtain.OnObtain

interface SessionOperationAsync {

    fun start(): Boolean

    fun perform(): Boolean

    fun end(callback: OnObtain<Boolean>)
}