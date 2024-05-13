package com.redelf.commons.persistance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.isEmpty
import com.redelf.commons.isNotEmpty
import timber.log.Timber
import java.sql.SQLException

/*
    TODO: Make sure that this is not static object
*/
internal object DBStorage : Storage<String> {

    /*
        TODO: Implement the mechanism to split data into chunks
    */

    private const val DATABASE_VERSION = 1

    private const val TABLE = "entries"
    private const val DATABASE_NAME = "Storage.DB"
    private const val DATABASE_NAME_SUFFIX_KEY = "DATABASE.NAME.SUFFIX.KEY"

    private const val COLUMN_VALUE = "content"
    private const val COLUMN_KEY = "identifier"

    private var prefs: SharedPreferencesStorage? = null

    private const val SQL_CREATE_ENTRIES = "CREATE TABLE $TABLE (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "$COLUMN_KEY TEXT," +
            "$COLUMN_VALUE TEXT)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE"

    private class DbHelper(context: Context, dbName: String) :

        SQLiteOpenHelper(

            context,
            dbName,
            null,
            DATABASE_VERSION

        ),

        ContextAvailability<BaseApplication>

    {

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

        override fun takeContext(): BaseApplication {

            return BaseApplication.takeContext()
        }
    }

    private var dbHelper: DbHelper? = null
    private var db: SQLiteDatabase? = null

    override fun initialize(ctx: Context) {

        val tag = "Initialize ::"

        Timber.v("$tag START")

        db?.let {

            if (db?.isOpen == true) {

                Timber.v("$tag ALREADY INITIALIZED")

                return
            }

            Timber.v("$tag AVAILABLE, BUT NOT OPEN")
        }

        try {

            val mainKey = "$DATABASE_NAME.$DATABASE_VERSION"
            val sPrefs = ctx.getSharedPreferences(mainKey, Context.MODE_PRIVATE)
            val nPrefs = SharedPreferencesStorage(sPrefs)

            prefs = nPrefs

            var suffix = nPrefs.get(DATABASE_NAME_SUFFIX_KEY)

            if (isEmpty(suffix)) {

                suffix = "_" + System.currentTimeMillis()

                if (!nPrefs.put(DATABASE_NAME_SUFFIX_KEY, suffix)) {

                    Timber.e("Error saving key preferences: $DATABASE_NAME_SUFFIX_KEY")
                }
            }

            val dbName = "$mainKey.$suffix"

            Timber.v("$tag dbName = $dbName")

            dbHelper = DbHelper(ctx, dbName)
            db = dbHelper?.writableDatabase

            Timber.v("$tag END")

        } catch (e: SQLException) {

            Timber.e(tag, e)
        }
    }

    override fun shutdown(): Boolean {

        db.let {

            try {

                it?.close()

                db = null

                return true

            } catch (e: Exception) {

                Timber.e(e)
            }
        }

        return false
    }

    override fun terminate(): Boolean {

        if (shutdown()) {

            return deleteDatabase()
        }

        return false
    }

    override fun put(key: String, value: String): Boolean {

        PersistenceUtils.checkNull("key", key)

        val tag = "Put :: $key ::"

        Timber.v("$tag START")

        if (db?.isOpen == false) {

            Timber.e("$tag DB is not open")
            return false
        }

        try {

            val values = ContentValues().apply {

                put(COLUMN_KEY, key)
                put(COLUMN_VALUE, value)
            }

            val selection = "$COLUMN_KEY = ?"
            val selectionArgs = arrayOf(key)

            val rowsUpdated = db?.update(

                TABLE,
                values,
                selection,
                selectionArgs

            ) ?: 0

            if (rowsUpdated > 0) {

                Timber.v("$tag END: rowsUpdated = $rowsUpdated, length = ${value.length}")

                return true
            }

            val rowsInserted = (db?.insert(TABLE, null, values) ?: 0)

            if (rowsInserted > 0) {

                Timber.v("$tag END: rowsInserted = $rowsInserted, length = ${value.length}")

                return true
            }

        } catch (e: Exception) {

            Timber.e(tag, e)
        }

        Timber.e(

            "$tag END :: Nothing was inserted or updated, length =" +
                    " ${value.length}, value = '$value'"
        )

        return false
    }

    override fun get(key: String): String {

        var result = ""
        val tag = "Get :: $key ::"
        val selectionArgs = arrayOf(key)
        val selection = "$COLUMN_KEY = ?"
        val projection = arrayOf(BaseColumns._ID, COLUMN_KEY, COLUMN_VALUE)

        Timber.v("$tag START")

        if (db?.isOpen == false) {

            Timber.w("$tag DB is not open")
            return result
        }

        try {

            val cursor = db?.query(

                TABLE,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            cursor?.let {

                with(it) {

                    while (moveToNext() && isEmpty(result)) {

                        result = getString(getColumnIndexOrThrow(COLUMN_VALUE))
                    }
                }
            }

            cursor?.close()

        } catch (e: Exception) {

            Timber.e(

                "$tag SQL args :: Selection: $selection, Selection args:" +
                        " ${selectionArgs.toMutableList()}, projection: " +
                        "${projection.toMutableList()}", e
            )
        }

        if (isNotEmpty(result)) {

            Timber.v("$tag END")

        } else {

            Timber.w("$tag END: Nothing found")
        }

        return result
    }

    override fun delete(key: String): Boolean {

        val tag = "Delete :: By key :: $key ::"

        Timber.v("$tag START")

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return false
        }

        val selection = "$COLUMN_KEY = ?"
        val selectionArgs = arrayOf(key)

        try {

            val result = (db?.delete(TABLE, selection, selectionArgs) ?: 0) > 0

            if (result) {

                Timber.v("$tag END")

            } else {

                Timber.e("$tag FAILED")
            }

            return result

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e(

                "$tag ERROR :: SQL args :: Selection: $selection, Selection args: " +
                    "${selectionArgs.toMutableList()}"
            )
        }

        return false
    }

    override fun deleteAll(): Boolean {

        val tag = "Delete :: All ::"

        Timber.v("$tag START")

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return false
        }

        try {

            val result = (db?.delete(TABLE, null, null) ?: 0) > 0

            if (result) {

                Timber.v("$tag END")

            } else {

                Timber.e("$tag FAILED")
            }

            return result

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e("$tag ERROR :: SQL args :: TO DELETE ALL")
        }

        return false
    }

    private fun deleteDatabase(): Boolean {

        val tag = "Delete :: Database ::"

        Timber.v("$tag START")

        try {

            val context = dbHelper?.takeContext()
            val result = context?.deleteDatabase(dbHelper?.databaseName) ?: false

            if (result) {

                Timber.v("$tag END")

                if (prefs?.delete(DATABASE_NAME_SUFFIX_KEY) != true) {

                    Timber.e("Error deleting key preferences: $DATABASE_NAME_SUFFIX_KEY")
                }

            } else {

                Timber.e("$tag FAILED")
            }

            return result

        } catch (e: Exception) {

            Timber.e(e)

            Timber.e("$tag ERROR :: SQL args :: TO DELETE DB")
        }

        return false
    }

    override fun contains(key: String): Boolean {

        return isNotEmpty(get(key))
    }

    override fun count(): Long {

        var result = 0L

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return 0
        }

        try {

            val projection = arrayOf(BaseColumns._ID, COLUMN_KEY, COLUMN_VALUE)

            val cursor = db?.query(

                TABLE,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            result = cursor?.count?.toLong() ?: 0

            cursor?.close()

        } catch (e: Exception) {

            Timber.e(e)
        }

        return result
    }
}
