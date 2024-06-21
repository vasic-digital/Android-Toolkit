package com.redelf.commons.migration

import com.redelf.commons.obtain.OnObtain

abstract class NoDataMigration(dataManagersReadyRequired: Boolean = true) :

    DataMigration<Unit, Unit>(dataManagersReadyRequired)

{

    override fun getTarget(source: Unit, callback: OnObtain<Unit>) = callback.onCompleted(Unit)

    override fun getSource(callback: OnObtain<Unit>) = callback.onCompleted(Unit)
}