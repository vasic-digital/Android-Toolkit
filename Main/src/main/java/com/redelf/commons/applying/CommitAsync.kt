package com.redelf.commons.applying

import com.redelf.commons.obtain.OnObtain

interface CommitAsync<R> : Committable {

    fun commit(from: String, callback: OnObtain<R?>)
}