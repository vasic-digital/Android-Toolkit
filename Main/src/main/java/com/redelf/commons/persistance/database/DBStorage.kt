package com.redelf.commons.persistance.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.text.isDigitsOnly
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Storage
import com.redelf.commons.persistance.encryption.NoEncryption
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import okio.IOException
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/*
    TODO: Make sure that this is not static object
*/
@SuppressLint("StaticFieldLeak")
object DBStorage : Storage<String> {

    /*
        TODO: Make possible for data managers to have multiple databases - each manager its own
    */

    val DEBUG = AtomicBoolean()


    private const val KEY_CHUNK = "chunk"
    private const val TAG = "DbStorage ::"
    private const val KEY_CHUNKS = "chunks"
    private const val DATABASE_VERSION = 1
    private const val DATABASE_NAME = "sdb"
    private const val MAX_CHUNK_SIZE = 5000

    private const val TABLE_ = "dt"
    private const val COLUMN_KEY_ = "ky"
    private const val COLUMN_VALUE_ = "ct"

    private var table = TABLE_
    private var columnKey = COLUMN_KEY_
    private var columnValue = COLUMN_VALUE_
    private val executor = Executor.MAIN
    private var enc: Encryption<String> = NoEncryption()
    private val getting = ConcurrentHashMap<String, Callbacks<OnObtain<String?>>>()

    private fun table() = table

    private fun columnValue() = columnValue

    private fun columnKey() = columnKey

    private fun sqlCreate() = "CREATE TABLE $table (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "$columnKey TEXT," +
            "$columnValue TEXT)"

    // Note: Not used at the moment
    // private fun sqlDelete() = "DROP TABLE IF EXISTS $table"

    private class DbHelper(context: Context, dbName: String) :

        SQLiteOpenHelper(

            context,
            dbName,
            null,
            DATABASE_VERSION
        ),

        ContextAvailability<BaseApplication> {

        override fun onCreate(db: SQLiteDatabase) {

            try {

                db.execSQL(sqlCreate())

            } catch (e: Throwable) {

                Console.error(e)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            if (DEBUG.get()) Console.log(

                "Old version: $oldVersion :: New version: $newVersion"
            )
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            onUpgrade(db, oldVersion, newVersion)
        }

        override fun takeContext(): BaseApplication {

            return BaseApplication.takeContext()
        }

        fun closeDatabase() {

            val latch = CountDownLatch(1)

            withDb { db ->

                try {

                    if (db?.isOpen == true) {

                        db.close()
                    }

                } catch (e: Throwable) {

                    Console.error(e)
                }

                latch.countDown()
            }

            latch.await()
        }
    }

    private fun mainKey() = "$DATABASE_NAME.$DATABASE_VERSION"

    private fun suffix() = DATABASE_NAME.hashCode().toString().reversed().substring(0, 2)

    private fun rawName() = "${mainKey()}.${suffix()}"

    private fun dbName() = "$DATABASE_NAME.$DATABASE_VERSION"

    private val dbHelper: DbHelper = DbHelper(BaseApplication.takeContext(), dbName())

    override fun initialize(ctx: Context) {

        val tag = "$TAG Initialize ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            table = table()
            enc = NoEncryption()
            columnKey = columnKey()
            columnValue = columnValue()

            if (DEBUG.get()) Console.log(

                "$tag dbName = '${dbName()}', rawName = '${rawName()}',"
            )

            if (DEBUG.get()) Console.log("$tag END")

        } catch (e: SQLException) {

            Console.error(tag, e.message ?: "Unknown error")

            Console.error(e)
        }
    }

    override fun shutdown(): Boolean {

        dbHelper.closeDatabase()

        return true
    }

    override fun terminate(vararg args: Any): Boolean {

        if (shutdown()) {

            return deleteDatabase()
        }

        return false
    }

    override fun put(key: String?, value: String): Boolean {

        if (isEmpty(key)) {

            return false
        }

        val tag =
            "$TAG Put :: DO :: $key :: column_key = $columnValue :: column_value = $columnValue ::"

        val chunks = value.chunked(MAX_CHUNK_SIZE)

        if (chunks.isEmpty()) {

            return false
        }

        val chunksCount = chunks.size

        doPut("${key}_$KEY_CHUNKS", chunksCount.toString())

        if (chunksCount > 1) {

            if (DEBUG.get()) Console.log("$tag START :: Chunk :: Chunks count = $chunksCount")

            var index = 0
            var success = true

            chunks.forEach { chunk ->

                if (success) {

                    if (doPut("${key}_${KEY_CHUNK}_$index", chunk)) {

                        if (DEBUG.get()) {

                            Console.log("$tag Chunk :: Written chunk = ${index + 1} / $chunksCount")
                        }

                    } else {

                        Console.error("$tag Chunk :: Not written chunk = ${index + 1} / $chunksCount")

                        success = false
                    }

                    index++
                }
            }

            return success

        } else {

            if (DEBUG.get()) Console.log("$tag START Chunk :: No chunks")

            return doPut("${key}_${KEY_CHUNK}_0", chunks[0])
        }
    }

    override fun get(key: String?, callback: OnObtain<String?>) {

        val tag = "$TAG Get :: Key='$key' ::"

        if (DEBUG.get()) {

            Console.log("$tag START")
        }

        exec(

            onRejected = { e ->

                Console.error("$tag REJECTED")
                callback.onFailure(e)
            }

        ) {

            if (isEmpty(key)) {

                val e = IllegalArgumentException("Empty key")
                Console.error("$tag FAILED :: Error='$e'")
                callback.onFailure(e)
                return@exec
            }

            if (DEBUG.get()) {

                Console.log("$tag STARTED")
            }

            try {

                if (DEBUG.get()) {

                    Console.log("$tag Going to call do get")
                }

                val doGetCallback = object : OnObtain<String?> {

                    override fun onCompleted(data: String?) {

                        var chunks = 1
                        val chunksRawValue = data
                        val condition =
                            chunksRawValue?.isNotEmpty() == true && chunksRawValue.isDigitsOnly()

                        if (condition) {

                            chunks = chunksRawValue.toInt()
                        }

                        if (chunks < 1) {

                            callback.onCompleted("")

                        } else if (chunks == 1) {

                            if (DEBUG.get()) Console.log("$tag START :: Chunk :: No chunks")

                            doGetAsync("${key}_${KEY_CHUNK}_0", callback)

                        } else {

                            if (DEBUG.get()) {

                                Console.log("$tag START :: Chunk :: Chunks count = $chunks")
                            }

                            val result = StringBuilder()
                            val pieces = ConcurrentHashMap<Int, String?>()

                            for (i in 0..chunks - 1) {

                                pieces[i] = doGet("${key}_${KEY_CHUNK}_$i")

                                if (pieces.size == chunks) {

                                    pieces.keys.toMutableList().sorted()
                                        .forEach { index ->

                                            val part = pieces[index]

                                            if (part?.isNotEmpty() == true) {

                                                result.append(part)

                                                if (DEBUG.get()) {

                                                    Console.log(

                                                        "$tag Chunk :: " +
                                                                "Loaded chunk = " +
                                                                "${index + 1} / $chunks"
                                                    )
                                                }
                                            }
                                        }
                                }
                            }

                            callback.onCompleted(result.toString())
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        Console.error("$tag FAILED :: Error='$error'")
                        callback.onFailure(error)
                    }
                }

                if (DEBUG.get()) {

                    Console.log("$tag Calling do get")
                }

                doGetAsync("${key}_$KEY_CHUNKS", doGetCallback)

            } catch (e: Throwable) {

                Console.error("$tag FAILED :: Error='$e'")
                callback.onFailure(e)
            }
        }
    }

    override fun delete(key: String?): Boolean {

        val tag = "$TAG Delete :: By key :: $key ::"

        if (isEmpty(key)) {

            Console.error("$tag Empty key")

            return false
        }

        if (DEBUG.get()) Console.log("$tag START")

        val result = AtomicBoolean()
        val latch = CountDownLatch(1)

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")

                latch.countDown()

                return@withDb
            }

            val res = transact(

                object : LocalDBStorageOperation<Boolean>(db) {

                    override fun perform(): Boolean {

                        val selection = "$columnKey = ?"
                        val selectionArgs = arrayOf(key)

                        try {

                            val rowsCount = db?.delete(table, selection, selectionArgs) ?: 0

                            if (DEBUG.get()) Console.log("$tag END :: Rows affected = $rowsCount")

                            return true

                        } catch (e: Throwable) {

                            Console.error(

                                "$tag ERROR :: SQL args :: Selection: $selection, " +
                                        "Selection args: " +
                                        "${selectionArgs.toMutableList()}",

                                e.message ?: "Unknown error"
                            )

                            Console.error(e)
                        }

                        return false
                    }
                }

            ) ?: false

            result.set(res)

            latch.countDown()
        }

        latch.await()

        return result.get()
    }

    override fun deleteAll(): Boolean {

        val tag = "$TAG Delete :: All ::"

        if (DEBUG.get()) Console.log("$tag START")

        val result = AtomicBoolean()
        val latch = CountDownLatch(1)

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")

                latch.countDown()

                return@withDb
            }

            val res = transact(

                object : LocalDBStorageOperation<Boolean>(db) {

                    override fun perform(): Boolean {

                        try {

                            val res = (db?.delete(table, null, null) ?: 0) > 0

                            if (res) {

                                if (DEBUG.get()) Console.log("$tag END")

                            } else {

                                Console.error("$tag FAILED")
                            }

                            return res

                        } catch (e: Throwable) {

                            Console.error(

                                "$tag ERROR :: SQL args :: " +
                                        "TO DELETE ALL", e.message ?: "Unknown error"
                            )

                            Console.error(e)
                        }

                        return false
                    }
                }

            ) ?: false

            result.set(res)

            latch.countDown()
        }

        latch.await()

        return result.get()
    }

    private fun deleteDatabase(): Boolean {

        val tag = "$TAG Delete :: Database ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            val dbName = dbHelper.databaseName
            val context = dbHelper.takeContext()
            val result = context.deleteDatabase(dbName)

            if (result) {

                if (DEBUG.get()) Console.log(

                    "$tag END: DB '$dbName' has been deleted"
                )

            } else {

                Console.error("$tag FAILED")
            }

            return result

        } catch (e: Throwable) {

            Console.error(

                "$tag ERROR :: SQL args :: " +
                        "TO DELETE DB", e.message ?: "Unknown error"
            )

            Console.error(e)
        }

        return false
    }

    override fun contains(key: String?, callback: OnObtain<Boolean?>) {

        get(

            key,

            object : OnObtain<String?> {

                override fun onCompleted(data: String?) {

                    callback.onCompleted(data?.isNotEmpty() == true)
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun count(): Long {

        val result = AtomicLong()
        val latch = CountDownLatch(1)

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")

                latch.countDown()

                return@withDb
            }

            try {

                val projection = arrayOf(BaseColumns._ID, columnKey, columnValue)

                val cursor = db?.query(

                    table,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null
                )

                val res = cursor?.count?.toLong() ?: 0
                result.set(res)

                cursor?.close()

            } catch (e: Throwable) {

                Console.error(e)
            }

            latch.countDown()
        }

        latch.await()

        return result.get()
    }

    private abstract class LocalDBStorageOperation<T>(db: SQLiteDatabase?) :
        DBStorageOperation<T>(db)

    private fun <T> transact(operation: DBStorageOperation<T>): T? {

        val db = operation.db
        var success: T? = null

        db?.let {

            try {

                it.beginTransaction()

                success = operation.perform()

                success?.let {

                    db.setTransactionSuccessful()
                }

            } catch (e: Throwable) {

                Console.error(e)

            } finally {

                try {

                    db.endTransaction()

                } catch (e: Throwable) {

                    Console.error(e)
                }
            }
        }

        return success
    }

    private fun withDb(tag: String = "", doWhat: (db: SQLiteDatabase?) -> Unit) {

        exec(

            onRejected = { e -> recordException(e) }

        ) {

            var tag = "$tag With DB ::".trim()

            try {

                val db = dbHelper.writableDatabase

                tag = "$tag db = ${db.hashCode()} ::"

                if (DEBUG.get()) Console.log("$tag START")

                db.let {

                    if (db.isOpen) {

                        if (DEBUG.get()) Console.log("$tag EXECUTING")

                        executor.execute {

                            doWhat(db)

                            if (DEBUG.get()) Console.log("$tag EXECUTED")
                        }

                    } else {

                        Console.warning("$tag DB is not open")
                    }
                }

            } catch (e: Throwable) {

                Console.error("$tag ERROR ::", e.message ?: "Unknown error")

                Console.error(e)
            }
        }
    }

    private fun doPut(key: String?, value: String): Boolean {

        if (isEmpty(key)) {

            return false
        }

        val tag =
            "$TAG Put :: DO :: $key :: column_key = $columnValue :: column_value = $columnValue ::"

        if (DEBUG.get()) Console.log("$tag START")

        if (isOnMainThread()) {

            val e = IllegalArgumentException("Do put from the main thread")
            Console.error(e)
        }

        val result = AtomicBoolean()
        val latch = CountDownLatch(1)

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")

                latch.countDown()

                return@withDb
            }

            val res = transact(

                object : LocalDBStorageOperation<Boolean>(db) {

                    override fun perform(): Boolean {

                        try {

                            val values = ContentValues().apply {

                                put(columnKey, key)
                                put(columnValue, value)
                            }

                            val selection = "$columnKey = ?"
                            val selectionArgs = arrayOf(key)

                            val rowsUpdated = db?.update(

                                table,
                                values,
                                selection,
                                selectionArgs

                            ) ?: 0

                            if (rowsUpdated > 0) {

                                if (DEBUG.get()) Console.log(

                                    "$tag END: rowsUpdated = $rowsUpdated, " +
                                            "length = ${value.length}"
                                )

                                return true
                            }

                            val rowsInserted = (db?.insert(table(), null, values) ?: 0)

                            if (rowsInserted > 0) {

                                if (DEBUG.get()) Console.log(

                                    "$tag END: rowsInserted = $rowsInserted, " +
                                            "length = ${value.length}"
                                )

                                return true
                            }

                        } catch (e: Throwable) {

                            Console.error(tag, e.message ?: "Unknown error")

                            Console.error(e)
                        }

                        Console.error(

                            "$tag END :: Nothing was inserted or updated, " +
                                    "length = ${value.length}"
                        )

                        return false
                    }
                }

            ) ?: false

            result.set(res)

            latch.countDown()
        }

        latch.await()

        return result.get()
    }

    private fun doGet(key: String?): String? {

        if (isOnMainThread()) {

            val e = IllegalArgumentException("Do get from the main thread")
            recordException(e)
        }

        return sync("DBStorage.doGet.$key", "") { callback ->

            doGetAsync(key, callback)
        }
    }

    @Synchronized
    private fun doGetAsync(key: String?, getterCallback: OnObtain<String?>) {

        val tag = "$TAG Do get async :: Key='$key' ::"

        if (isEmpty(key)) {

            val e = IllegalArgumentException("Empty key")
            Console.error("$tag FAILED :: Error='$e'")
            getterCallback.onFailure(e)
            return
        }

        key?.let {

            var callbacks = getting[key]

            fun register(callbacks: Callbacks<OnObtain<String?>>): Boolean {

                if (callbacks.isRegistered(getterCallback)) {

                    if (DEBUG.get()) {

                        Console.warning("$tag Already registered}")
                    }

                    return true
                }

                callbacks.register(getterCallback)

                return false
            }

            if (callbacks == null) {

                callbacks = Callbacks("getting.$key")
                getting[key] = callbacks
            }

            val inProgress = callbacks.getSubscribersCount() > 0

            register(callbacks)

            if (inProgress) {

                Console.warning("$tag Already getting :: Key='$key'")

                return
            }

            if (DEBUG.get()) {

                Console.log("$tag START")
            }

            fun notifyGetterCallback(data: String? = null, error: Throwable? = null) {

                if (DEBUG.get()) {

                    Console.log("$tag Notify getter callbacks :: Count=${callbacks.size()}")
                }

                callbacks.doOnAll(object : CallbackOperation<OnObtain<String?>> {

                    override fun perform(callback: OnObtain<String?>) {

                        error?.let {

                            callback.onFailure(it)

                        }

                        if (error == null) {

                            callback.onCompleted(data)
                        }
                    }

                }, operationName = "getting.$key")

                callbacks.clear()

                getting.remove(key)
            }

            exec(

                onRejected = { e ->

                    Console.error("$tag REJECTED")

                    notifyGetterCallback(error = e)
                }

            ) {

                if (DEBUG.get()) Console.log("$tag STARTED")

                withDb(tag) { db ->

                    if (DEBUG.get()) Console.log("$tag Got DB")

                    var result = ""
                    val selection = "$columnKey = ?"
                    val selectionArgs = arrayOf(key)
                    val projection = arrayOf(BaseColumns._ID, columnKey, columnValue)

                    if (db?.isOpen == false) {

                        val e = IOException("DB is not open")
                        Console.error("$tag FAILED :: Error='${e.message}'")
                        notifyGetterCallback(error = e)
                        return@withDb
                    }

                    var cursor: Cursor? = null

                    try {

                        cursor = db?.query(

                            table,
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

                                    try {

                                        val idx = getColumnIndexOrThrow(columnValue)
                                        result = getString(idx)

                                    } catch (e: Throwable) {

                                        notifyGetterCallback(error = e)
                                        result = e.message ?: "error"
                                    }
                                }
                            }
                        }

                        cursor?.close()

                    } catch (e: Throwable) {

                        notifyGetterCallback(error = e)
                        return@withDb

                    } finally {

                        try {

                            cursor?.close()

                        } catch (e: Throwable) {

                            recordException(e)
                        }
                    }

                    notifyGetterCallback(data = result)
                }
            }
        }
    }
}
