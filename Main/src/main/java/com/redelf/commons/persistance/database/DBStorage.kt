package com.redelf.commons.persistance.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.text.isDigitsOnly
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.CountDownLatch
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.read
import kotlin.concurrent.write

class DBStorage private constructor(context: Context) : Storage<String> {

    companion object {
        @Volatile
        private var INSTANCE: DBStorage? = null

        private const val KEY_CHUNK = "chunk"
        private const val TAG = "DbStorage ::"
        private const val KEY_CHUNKS = "chunks"
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "sdb"
        private const val MAX_CHUNK_SIZE = 500000 // 500KB chunks to handle large JSON safely
        private const val MAX_SCHEDULE_SIZE = 10000
        private const val MAX_CHUNKS_PER_KEY = 1000
        private const val DB_OPERATION_TIMEOUT_MS = 30000L
        private const val TABLE_ = "dt"
        private const val COLUMN_KEY_ = "ky"
        private const val COLUMN_VALUE_ = "ct"

        fun getInstance(context: Context): DBStorage {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: DBStorage(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun resetInstance() {

            synchronized(this) {

                INSTANCE?.shutdown()
                INSTANCE = null
            }
        }
    }

    val DEBUG = AtomicBoolean()

    private var table = TABLE_
    private var columnKey = COLUMN_KEY_
    private val executor = Executor.MAIN
    private var columnValue = COLUMN_VALUE_
    private val processing = AtomicBoolean()
    private var enc: Encryption<String> = NoEncryption()
    private val schedule = ConcurrentHashMap<String, String>()
    private val scheduleAccessLock = ReentrantReadWriteLock()
    private val dbAccessSemaphore = Semaphore(50)
    private val appContext = context

    private fun table() = table

    private fun columnValue() = columnValue

    private fun columnKey() = columnKey

    private fun sqlCreate() = "CREATE TABLE $table (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "$columnKey TEXT," +
            "$columnValue TEXT)"


    // Note: Not used at the moment
    // private fun sqlDelete() = "DROP TABLE IF EXISTS $table"

    private inner class DbHelper(context: Context, dbName: String) :

        SQLiteOpenHelper(

            context,
            dbName,
            null,
            DATABASE_VERSION
        ),

        ContextAvailability<BaseApplication>

    {

        private val helperContext = context

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

            return helperContext as BaseApplication
        }

        fun closeDatabase() {

            val latch = CountDownLatch(1, "DbStorage.DbHelper.closeDatabase")

            withDb { db ->

                try {

                    if (db?.isOpen == true) {

                        db.close()
                    }

                } catch (e: Throwable) {

                    Console.error(e)

                } finally {

                    latch.countDown()
                }
            }

            try {

                if (!latch.await(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                    Console.error("Database close operation timed out")
                }

            } catch (e: InterruptedException) {

                Thread.currentThread().interrupt()
                Console.error("Database close operation interrupted", e)
            }
        }
    }

    private fun mainKey() = "$DATABASE_NAME.$DATABASE_VERSION"

    private fun suffix() = DATABASE_NAME.hashCode().toString().reversed().substring(0, 2)

    private fun rawName() = "${mainKey()}.${suffix()}"

    private fun dbName() = "$DATABASE_NAME.$DATABASE_VERSION"

    private val dbHelper: DbHelper = DbHelper(appContext, dbName())

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

        if (chunksCount > MAX_CHUNKS_PER_KEY) {

            Console.error("$tag Chunk count exceeds maximum allowed: $chunksCount > $MAX_CHUNKS_PER_KEY")
            return false
        }

        if (!doSchedule("${key}_$KEY_CHUNKS", chunksCount.toString())) {

            return false
        }

        if (chunksCount > 1) {

            if (DEBUG.get()) Console.log("$tag START :: Chunk :: Chunks count = $chunksCount")

            var index = 0
            var success = true

            for (chunk in chunks) {

                if (!success) break

                if (doSchedule("${key}_${KEY_CHUNK}_$index", chunk)) {

                    if (DEBUG.get()) {

                        Console.log("$tag Chunk :: Written chunk = ${index + 1} / $chunksCount")
                    }

                } else {

                    Console.error("$tag Chunk :: Not written chunk = ${index + 1} / $chunksCount")
                    success = false
                }

                index++
            }

            return success

        } else {

            if (DEBUG.get()) Console.log("$tag START Chunk :: No chunks")

            return doSchedule("${key}_${KEY_CHUNK}_0", chunks[0])
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
                            val pieces = Array<String?>(chunks) { null }

                            for (i in 0 until chunks) {

                                pieces[i] = doGet("${key}_${KEY_CHUNK}_$i")
                            }

                            for (i in 0 until chunks) {

                                val part = pieces[i]

                                if (part?.isNotEmpty() == true) {

                                    result.append(part)

                                    if (DEBUG.get()) {

                                        Console.log(
                                            "$tag Chunk :: Loaded chunk = ${i + 1} / $chunks"
                                        )
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
        val latch = CountDownLatch(1, "DbStorage.delete(key='$key')")

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

        try {

            if (!latch.await(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                Console.error("Database operation timed out")
            }

        } catch (e: InterruptedException) {

            Thread.currentThread().interrupt()
            Console.error("Database operation interrupted", e)
        }

        return result.get()
    }

    override fun deleteAll(): Boolean {

        val tag = "$TAG Delete :: All ::"

        if (DEBUG.get()) Console.log("$tag START")

        val result = AtomicBoolean()
        val latch = CountDownLatch(1, "DbStorage.DeleteAll")

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

        try {

            if (!latch.await(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                Console.error("Database operation timed out")
            }

        } catch (e: InterruptedException) {

            Thread.currentThread().interrupt()
            Console.error("Database operation interrupted", e)
        }

        return result.get()
    }

    private fun deleteDatabase(): Boolean {

        val tag = "$TAG Delete :: Database ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            val dbName = dbHelper.databaseName
            val contextRef = dbHelper.takeContext()
            val result = contextRef.deleteDatabase(dbName)

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
        val latch = CountDownLatch(1, "DbStorage.count")

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")

                latch.countDown()

                return@withDb
            }

            try {

                val projection = arrayOf(BaseColumns._ID, columnKey, columnValue)

                var cursor: Cursor? = null

                try {

                    cursor = db?.query(

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

                } finally {

                    cursor?.close()
                }

            } catch (e: Throwable) {

                Console.error(e)
            }

            latch.countDown()
        }

        try {

            if (!latch.await(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                Console.error("Database operation timed out")
            }

        } catch (e: InterruptedException) {

            Thread.currentThread().interrupt()
            Console.error("Database operation interrupted", e)
        }

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

            var tagMsg = "$tag With DB ::".trim()

            try {

                if (!dbAccessSemaphore.tryAcquire(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                    Console.error("$tagMsg Database access timeout - too many concurrent operations")
                    return@exec
                }

                try {

                    val db = dbHelper.writableDatabase
                    tagMsg = "$tagMsg db = ${db.hashCode()} ::"

                    if (DEBUG.get()) Console.log("$tagMsg START")

                    if (db.isOpen) {

                        if (DEBUG.get()) Console.log("$tagMsg EXECUTING")

                        executor.execute {

                            try {

                                doWhat(db)

                            } finally {

                                dbAccessSemaphore.release()
                            }

                            if (DEBUG.get()) Console.log("$tagMsg EXECUTED")
                        }

                    } else {

                        Console.warning("$tagMsg DB is not open")
                        dbAccessSemaphore.release()
                    }

                } catch (e: Throwable) {

                    dbAccessSemaphore.release()
                    throw e
                }

            } catch (_: InterruptedException) {

                Thread.currentThread().interrupt()
                Console.error("$tagMsg Interrupted while waiting for database access")

            } catch (e: Throwable) {

                Console.error("$tagMsg ERROR ::", e.message ?: "Unknown error")
                Console.error(e)
            }
        }
    }

    private fun doSchedule(key: String?, value: String): Boolean {

        if (key == null) return false

        return scheduleAccessLock.write {
            try {

                if (schedule.size >= MAX_SCHEDULE_SIZE) {

                    Console.error("Schedule queue is full. Rejecting new entries to prevent memory issues.")
                    return@write false
                }

                schedule[key] = value
                doProcess()

                true

            } catch (e: Throwable) {

                recordException(e)
                false
            }
        }
    }

    private fun doProcess() {

        exec {

            if (processing.get()) {

                return@exec
            }

            processing.set(true)

            try {

                scheduleAccessLock.read {

                    if (schedule.isEmpty()) {

                        if (DEBUG.get()) {

                            Console.log("Schedule is empty, nothing to process")
                        }

                        return@exec
                    }
                }

                val entriesToProcess = scheduleAccessLock.read {

                    schedule.toMap()
                }

                val successfulKeys = mutableSetOf<String>()

                entriesToProcess.forEach { (key, value) ->

                    if (doPut(key, value)) {

                        successfulKeys.add(key)

                    } else {

                        if (DEBUG.get()) {

                            Console.warning("Put :: Failed :: Key='$key' :: Will retry later")
                        }
                    }
                }

                if (successfulKeys.isNotEmpty()) {

                    scheduleAccessLock.write {

                        successfulKeys.forEach { key ->

                            schedule.remove(key)
                        }
                    }
                }

                scheduleAccessLock.read {

                    if (schedule.isEmpty()) {

                        if (DEBUG.get()) {

                            Console.log("Schedule is empty, everything is processed")
                        }
                    }
                }

            } finally {

                processing.set(false)
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

        val result = AtomicBoolean()
        val cDown = CountDownLatch(1, "DBStorage.doPut(key='$key')")

        withDb { db ->

            if (db?.isOpen == false) {

                Console.warning("DB is not open")
                cDown.countDown()

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

                                    "$tag END: rowsUpdated = $rowsUpdated, length = ${value.length}"
                                )

                                return true
                            }

                            val rowsInserted = db?.insert(table(), null, values) ?: -1

                            if (rowsInserted > 0) {

                                if (DEBUG.get()) Console.log(

                                    "$tag END: rowsInserted = $rowsInserted, length = ${value.length}"
                                )

                                return true
                            }

                        } catch (e: Throwable) {

                            Console.error(tag, e.message ?: "Unknown error")
                            Console.error(e)
                        }

                        Console.error(

                            "$tag END :: Nothing was inserted or updated, length = ${value.length}"
                        )

                        return false
                    }
                }

            ) ?: false

            result.set(res)
            cDown.countDown()
        }

        try {

            if (!cDown.await(DB_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {

                Console.error("$tag Database operation timed out")
                return false
            }

        } catch (e: InterruptedException) {

            Thread.currentThread().interrupt()
            Console.error("$tag Database operation interrupted", e)
            return false
        }

        return result.get()
    }

    private fun doGet(key: String?): String? {

        if (isOnMainThread()) {

            val e = IllegalArgumentException("Do get from the main thread")
            recordException(e)
            throw e
        }

        return sync("DBStorage.doGet.$key", "") { callback ->

            doGetAsync(key, callback)
        }
    }

    private fun doGetAsync(key: String?, getterCallback: OnObtain<String?>) {

        val tag = "$TAG Do get async :: Key='$key' ::"

        if (isEmpty(key)) {

            val e = IllegalArgumentException("Empty key")
            Console.error("$tag FAILED :: Error='$e'")
            getterCallback.onFailure(e)
            return
        }

        key?.let {

            if (DEBUG.get()) {

                Console.log("$tag START")
            }

            exec(

                onRejected = { e ->

                    Console.error("$tag REJECTED")

                    getterCallback.onFailure(e)
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
                        getterCallback.onFailure(e)
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
                                        result = getString(idx) ?: ""

                                    } catch (e: Throwable) {

                                        getterCallback.onFailure(e)
                                        return@withDb
                                    }
                                }
                            }
                        }

                        cursor?.close()

                    } catch (e: Throwable) {

                        getterCallback.onFailure(e)
                        return@withDb

                    } finally {

                        try {

                            cursor?.close()

                        } catch (e: Throwable) {

                            recordException(e)
                        }
                    }

                    getterCallback.onCompleted(result)
                }
            }
        }
    }
}
