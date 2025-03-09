package com.redelf.commons.persistance.database

import android.database.sqlite.SQLiteDatabase

abstract class DBStorageOperation<T>(val db: SQLiteDatabase?) {

    abstract fun perform(): T?
}