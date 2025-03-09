package com.redelf.commons.persistance.database

import net.zetetic.database.sqlcipher.SQLiteDatabase

internal abstract class DBStorageOperation<T>(val db: SQLiteDatabase?) {

    abstract fun perform(): T?
}