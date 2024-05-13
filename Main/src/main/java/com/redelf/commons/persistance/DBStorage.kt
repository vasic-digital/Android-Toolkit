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
import com.redelf.commons.randomInteger
import timber.log.Timber
import java.lang.StringBuilder
import java.sql.SQLException

/*
    TODO: Make sure that this is not static object
*/
internal object DBStorage : Storage<String> {

    /*
        TODO: Implement the mechanism to split data into chunks
    */

    private const val DATABASE_VERSION = 1

    private const val DATABASE_NAME = "Storage.DB"
    private const val DATABASE_NAME_SUFFIX_KEY = "DATABASE.NAME.SUFFIX.KEY"

    private const val TABLE_ = "dt"
    private const val COLUMN_KEY_ = "ky"
    private const val COLUMN_VALUE_ = "ct"

    private var enc: Encryption = NoEncryption()
    private var prefs: SharedPreferencesStorage? = null

    fun getString(source: String, key: String = DATABASE_NAME): String {

        var result = source

        try {

            val bytes = enc.encrypt(key, source)

            bytes?.let {

                result = String(bytes)
            }

        } catch (e: Exception) {

            Timber.e(e)
        }

        return result
    }

    private fun table() = getString(TABLE_)

    private fun columnValue() = getString(COLUMN_VALUE_)

    private fun columnKey() = getString(COLUMN_KEY_)

    private val SQL_CREATE_ENTRIES = "CREATE TABLE ${table()} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${columnKey()} TEXT," +
            "${columnValue()} TEXT)"

    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${table()}"

    private class DbHelper(context: Context, dbName: String) :

        SQLiteOpenHelper(

            context,
            dbName,
            null,
            DATABASE_VERSION
        ),

        ContextAvailability<BaseApplication> {

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

        fun closeDatabase() {

            val db = readableDatabase

            if (db.isOpen) {

                db.close()
            }
        }
    }

    private var dbHelper: DbHelper? = null

    override fun initialize(ctx: Context) {

        val tag = "Initialize ::"

        Timber.v("$tag START")

        try {

            val mainKey = "$DATABASE_NAME.$DATABASE_VERSION"
            val sPrefs = ctx.getSharedPreferences(mainKey, Context.MODE_PRIVATE)
            val nPrefs = SharedPreferencesStorage(sPrefs)

            var suffix = nPrefs.get(DATABASE_NAME_SUFFIX_KEY)

            if (isEmpty(suffix)) {

                suffix = "_" + System.currentTimeMillis()

                if (!nPrefs.put(DATABASE_NAME_SUFFIX_KEY, suffix)) {

                    Timber.e("Error saving key preferences: $DATABASE_NAME_SUFFIX_KEY")
                }
            }

            prefs = nPrefs

            enc = object : Encryption {

                override fun init() = true

                override fun encrypt(key: String?, value: String?): ByteArray {

                    fun getRandom() = randomInteger(max = 110, min = 11)

                    val separator = "_"
                    val builder = StringBuilder()

                    value?.forEach { letter ->

                        builder.append(letter)
                            .append(separator)
                            .append(getRandom())
                    }

                    return builder.toString().toByteArray()
                }

                override fun decrypt(key: String?, value: ByteArray?) : String {

                    return value?.let { String(it) } ?: ""
                }
            }

            val rawName = "$mainKey.$suffix"
            val dbName = getString(suffix, rawName)

            Timber.v("$tag dbName = '$dbName', rawName = '$rawName'")

            dbHelper = DbHelper(ctx, dbName)

            Timber.v("$tag END")

        } catch (e: SQLException) {

            Timber.e(tag, e)
        }
    }

    override fun shutdown(): Boolean {

        dbHelper?.closeDatabase()

        return true
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

        val db = take()

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return false
        }

        return transact(

            object : LocalDBStorageOperation<Boolean>() {

                override fun perform(): Boolean {

                    try {

                        val values = ContentValues().apply {

                            put(columnKey(), key)
                            put(columnValue(), value)
                        }

                        val selection = "${columnKey()} = ?"
                        val selectionArgs = arrayOf(key)

                        val rowsUpdated = db?.update(

                            table(),
                            values,
                            selection,
                            selectionArgs

                        ) ?: 0

                        if (rowsUpdated > 0) {

                            Timber.v(

                                "$tag END: rowsUpdated = $rowsUpdated, " +
                                        "length = ${value.length}"
                            )

                            return true
                        }

                        val rowsInserted = (db?.insert(table(), null, values) ?: 0)

                        if (rowsInserted > 0) {

                            Timber.v(

                                "$tag END: rowsInserted = $rowsInserted, " +
                                        "length = ${value.length}"
                            )

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
            }

        ) ?: false
    }

    override fun get(key: String): String {

        var result = ""
        val tag = "Get :: $key ::"
        val selectionArgs = arrayOf(key)
        val selection = "${columnKey()} = ?"
        val projection = arrayOf(BaseColumns._ID, columnKey(), columnValue())

        Timber.v("$tag START")

        val db = take()

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return result
        }

        try {

            val cursor = db?.query(

                table(),
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

                        result = getString(getColumnIndexOrThrow(columnValue()))
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

        val db = take()

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return false
        }

        return transact(

            object : LocalDBStorageOperation<Boolean>() {

                override fun perform(): Boolean {

                    val selection = "${columnKey()} = ?"
                    val selectionArgs = arrayOf(key)

                    try {

                        val result = (db?.delete(table(), selection, selectionArgs) ?: 0) > 0

                        if (result) {

                            Timber.v("$tag END")

                        } else {

                            Timber.e("$tag FAILED")
                        }

                        return result

                    } catch (e: Exception) {

                        Timber.e(

                            "$tag ERROR :: SQL args :: Selection: $selection, " +
                                    "Selection args: " +
                                    "${selectionArgs.toMutableList()}", e
                        )
                    }

                    return false
                }
            }

        ) ?: false
    }

    override fun deleteAll(): Boolean {

        val tag = "Delete :: All ::"

        Timber.v("$tag START")

        val db = take()

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return false
        }

        return transact(

            object : LocalDBStorageOperation<Boolean>() {

                override fun perform(): Boolean {

                    try {

                        val result = (db?.delete(table(), null, null) ?: 0) > 0

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
            }

        ) ?: false
    }

    private fun deleteDatabase(): Boolean {

        val tag = "Delete :: Database ::"

        Timber.v("$tag START")

        try {

            val dbName = dbHelper?.databaseName
            val context = dbHelper?.takeContext()
            val result = context?.deleteDatabase(dbName) ?: false

            if (result) {

                Timber.v("$tag END: DB '$dbName' has been deleted")

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

        val db = take()

        if (db?.isOpen == false) {

            Timber.w("DB is not open")
            return 0
        }

        var result = 0L

        try {

            val projection = arrayOf(BaseColumns._ID, columnKey(), columnValue())

            val cursor = db?.query(

                table(),
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

    private fun take(): SQLiteDatabase? {

        try {

            return dbHelper?.writableDatabase

        } catch (e: Exception) {

            Timber.e(e)
        }

        return null
    }

    private abstract class LocalDBStorageOperation<T> : DBStorageOperation<T>(db = take())

    private fun <T> transact(operation: DBStorageOperation<T>): T? {

        val db = operation.db
        var success: T? = null

        db?.let {

            it.beginTransaction()

            try {

                success = operation.perform()

                success?.let {

                    db.setTransactionSuccessful()
                }

            } catch (e: Exception) {

                Timber.e(e)

            } finally {

                try {

                    db.endTransaction()

                } catch (e: Exception) {

                    Timber.e(e)
                }
            }
        }

        return success
    }
}
