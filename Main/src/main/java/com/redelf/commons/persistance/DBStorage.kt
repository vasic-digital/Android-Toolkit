package com.redelf.commons.persistance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.redelf.commons.isEmpty
import com.redelf.commons.isNotEmpty
import timber.log.Timber

internal class DBStorage(context: Context) : Storage<String> {

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

    companion object {

        const val TABLE = "entries"
        const val COLUMN_VALUE = "content"
        const val COLUMN_KEY = "identifier"

        private val SQL_CREATE_ENTRIES = "CREATE TABLE $TABLE (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "$COLUMN_KEY TEXT," +
                    "$COLUMN_VALUE TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE"
    }

    override fun put(key: String, value: String): Boolean {

        PersistenceUtils.checkNull("key", key)

        try {

            val db = dbHelper.writableDatabase

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

        try {

            val db = dbHelper.readableDatabase
            val projection = arrayOf(BaseColumns._ID, COLUMN_KEY, COLUMN_VALUE)

            val selection = "$COLUMN_KEY = ?"
            val selectionArgs = arrayOf(key)

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
        }

        return result
    }

    override fun delete(key: String): Boolean {

        try {

            val db = dbHelper.readableDatabase
            val selection = "$COLUMN_KEY = ?"
            val selectionArgs = arrayOf(key)

            return db.delete(TABLE, selection, selectionArgs) > 0

        } catch (e: Exception) {

            Timber.e(e)
        }

        return false
    }

    override fun contains(key: String): Boolean {

        return isNotEmpty(get(key))
    }

    override fun deleteAll(): Boolean {

        try {

            val db = dbHelper.readableDatabase

            return db.delete(TABLE, null, null) > 0

        } catch (e: Exception) {

            Timber.e(e)
        }

        return false
    }

    override fun count(): Long {

        var result = 0L

        try {

            val db = dbHelper.readableDatabase
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
