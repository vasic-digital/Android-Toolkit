package com.redelf.commons.persistance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.isEmpty
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.recordException
import timber.log.Timber
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean

internal object DBStorage : Storage<String> {

    private const val DATABASE_VERSION = 1
    private const val DATABASE_NAME = "Storage.DB"

    private const val TABLE = "entries"
    private const val COLUMN_VALUE = "content"
    private const val COLUMN_KEY = "identifier"

    private const val SQL_CREATE_ENTRIES = "CREATE TABLE $TABLE (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "$COLUMN_KEY TEXT," +
            "$COLUMN_VALUE TEXT)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE"

    private class DbHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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

    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    override fun initialize(ctx: Context) {

        try {

            dbHelper = DbHelper(ctx)
            db = dbHelper.writableDatabase

        } catch (e: SQLException) {

            recordException(e)
        }
    }

    override fun shutdown(): Boolean {

        db.let {

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

        return isNotEmpty(get(key))
    }

    override fun deleteAll(): Boolean {

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
