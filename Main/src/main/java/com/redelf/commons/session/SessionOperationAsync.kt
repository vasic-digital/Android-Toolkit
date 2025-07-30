package com.redelf.commons.session

import com.redelf.commons.obtain.OnObtain

interface SessionOperationAsync : SessionOperation {

    fun end(notify: Boolean, callback: OnObtain<Boolean?>)
}