package com.redelf.commons.persistance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.redelf.commons.isEmpty
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.TerminationSynchronized
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal class DBStorage(context: Context) : Storage<String>, TerminationSynchronized {

    private val terminated = AtomicBoolean()

    private class DbHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        companion object {

            const val DATABASE_VERSION = 1
            const val DATABASE_NAME = "Storage.DB"
        }
        override fun onCreate(db: SQLiteDatabase) {

            db.execSQL(SQL_CREATE_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            db.execSQL(SQL_DELETE_ENTRIES)

            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            onUpgrade(db, oldVersion, newVersion)
        }
    }

    private val dbHelper = DbHelper(context)
    private val db = dbHelper.writableDatabase

    companion object {

        const val TABLE = "entries"
        const val COLUMN_VALUE = "content"
        const val COLUMN_KEY = "identifier"

        private const val SQL_CREATE_ENTRIES = "CREATE TABLE $TABLE (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "$COLUMN_KEY TEXT," +
                    "$COLUMN_VALUE TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE"
    }

    override fun shutdown(): Boolean {

        if (terminated.get()) {

            return true
        }

        terminated.set(true)

        db?.let {

            try {

                it.close()

                return true

            } catch (e: Exception) {

                Timber.e(e)
            }
        }

        return false
    }

    override fun put(key: String, value: String): Boolean {

        if (terminated.get()) {

            return false
        }

        PersistenceUtils.checkNull("key", key)

        try {

            val values = ContentValues().apply {

                put(COLUMN_KEY, key)
                put(COLUMN_VALUE, value)
            }

            return (db?.insert(TABLE, null, values) ?: -1) > 0

        } catch (e: Exception) {

            Timber.e(e)
        }

        return false
    }

    override fun get(key: String): String {

        var result = ""

        if (terminated.get()) {

            return result
        }

        val selectionArgs = arrayOf(key)
        val selection = "$COLUMN_KEY = ?"
        val projection = arrayOf(BaseColumns._ID, COLUMN_KEY, COLUMN_VALUE)

        try {

            val cursor = db.query(

                TABLE,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            with(cursor) {

                while (moveToNext() && isEmpty(result)) {

                    result = getString(getColumnIndexOrThrow(COLUMN_VALUE))
                }
            }

            cursor.close()

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e(

                "SQL args :: Selection: $selection, Selection args:" +
                        " ${selectionArgs.toMutableList()}, projection: " +
                        "${projection.toMutableList()}"
            )
        }

        return result
    }

    override fun delete(key: String): Boolean {

        if (terminated.get()) {

            return false
        }

        val selection = "$COLUMN_KEY = ?"
        val selectionArgs = arrayOf(key)

        try {

            return db.delete(TABLE, selection, selectionArgs) > 0

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e(

                "SQL args :: Selection: $selection, Selection args: " +
                    "${selectionArgs.toMutableList()}"
            )
        }

        return false
    }

    override fun contains(key: String): Boolean {

        if (terminated.get()) {

            return false
        }

        return isNotEmpty(get(key))
    }

    override fun deleteAll(): Boolean {

        if (terminated.get()) {

            return false
        }

        try {

            return db.delete(TABLE, null, null) > 0

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e("SQL args :: TO DELETE ALL")
        }

        return false
    }

    override fun count(): Long {

        var result = 0L

        if (terminated.get()) {

            return result
        }

        try {

            val projection = arrayOf(BaseColumns._ID, COLUMN_KEY, COLUMN_VALUE)

            val cursor = db.query(

                TABLE,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            result = cursor.count.toLong()

            cursor.close()

        } catch (e: Exception) {

            Timber.e(e)
        }

        return result
    }
}
