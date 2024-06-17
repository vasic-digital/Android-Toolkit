package com.redelf.commons.migration

import android.content.Context
import com.redelf.commons.obtain.OnObtain

abstract class NoDataMigration(private val ctx: Context) : DataMigration<Unit, Unit>() {

    override fun getTarget(source: Unit, callback: OnObtain<Unit>) = callback.onCompleted(Unit)

    override fun getSource(callback: OnObtain<Unit>) = callback.onCompleted(Unit)
}