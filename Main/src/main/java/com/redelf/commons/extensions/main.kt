@file:Suppress("DEPRECATION")

package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.database.Cursor
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StrictMode
import android.os.strictmode.Violation
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.Base64OutputStream
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.paging.DEBUG
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.execution.Execution
import com.redelf.commons.execution.Executor
import com.redelf.commons.execution.Executor.UI
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.PropertiesHash
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

val DEBUG_SYNC = AtomicBoolean()
val DEBUG_UI_SYNC = AtomicBoolean()
val DEFAULT_ACTIVITY_REQUEST = randomInteger()
var GLOBAL_RECORD_EXCEPTIONS = AtomicBoolean(true)
var GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK = AtomicBoolean()

fun randomInteger(max: Int = 1000, min: Int = 300) =
    Random().nextInt((max - min) + 1) + min

fun randomBigInteger(max: Int = 10000, min: Int = 300) =
    Random().nextInt((max - min) + 1) + min

fun randomFloat(max: Float = 1000f, min: Float = 300f) =
    Random().nextFloat() * (max - min) + min

fun generateValidColumnName(length: Int): String {

    val random = Random()
    val randomString = StringBuilder(length)

    for (i in 0 until length) {

        val randomChar = (random.nextInt(26) + 'a'.toInt()).toChar()
        randomString.append(randomChar)
    }

    return randomString.toString()
}

fun randomString(length: Int, sqliteFriendly: Boolean = true): String {

    if (sqliteFriendly) {

        return generateValidColumnName(length)
    }

    val random = Random()
    val randomString = StringBuilder(length)

    for (i in 0 until length) {

        val randomChar = (random.nextInt(26) + 'a'.code).toChar()
        randomString.append(randomChar)
    }

    return randomString.toString()
}

fun yieldWhile(condition: () -> Boolean) {

    // TODO: Support for the coroutines
    while (condition() && !Thread.currentThread().isInterrupted) {

        Thread.yield()
    }
}

fun yieldWhile(timeoutInMilliseconds: Long, condition: () -> Boolean) {

    val start = System.currentTimeMillis()

    while (

        condition() &&
        !Thread.currentThread().isInterrupted &&
        (System.currentTimeMillis() - start < timeoutInMilliseconds)

    ) {

        Thread.yield()
    }
}

fun recordException(e: Throwable) {

    Console.error(e)

    if (GLOBAL_RECORD_EXCEPTIONS.get()) {

        if (GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.get()) {

            throw e

        } else {

            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}

@Suppress("DEPRECATION")
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {

    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?

    manager?.let {

        for (service in it.getRunningServices(Int.MAX_VALUE)) {

            if (serviceClass.name == service.service.className) {

                return true
            }
        }
    }
    return false
}

@SuppressLint("Range")
fun Context.getFileName(uri: Uri): String? {

    var result: String? = null

    if (uri.scheme.equals("content")) {

        val cursor: Cursor? = contentResolver
            .query(uri, null, null, null, null)

        cursor.use {

            if (it != null && it.moveToFirst()) {

                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }

    if (result == null) {

        result = uri.path

        val cut = result?.lastIndexOf('/')

        if (cut != -1 && cut != null) {

            result = result.substring(cut + 1)
        }
    }

    return result
}

fun Context.closeKeyboard(v: View) {

    val inputMethodManager: InputMethodManager? =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

    inputMethodManager?.hideSoftInputFromWindow(v.applicationWindowToken, 0)
}

fun Context.showKeyboard(view: View) {

    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    view.requestFocus()

    inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.clearAllSharedPreferences(): Boolean {

    var result = true

    try {

        val sharedPreferencesNames = getSharedPreferences("", Context.MODE_PRIVATE).all.keys

        sharedPreferencesNames.forEach { sharedPreferencesName ->

            val res = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()

            if (!res) {

                result = false
            }
        }

    } catch (e: SecurityException) {

        Console.error(e)

        result = false
    }

    return result
}

fun Context.deobfuscateString(resId: Int, callback: OnObtain<String>) {

    try {

        return getString(resId).deobfuscate(callback = callback)

    } catch (e: NotFoundException) {

        callback.onFailure(e)
    }
}

fun Context.obfuscateString(resId: Int, callback: OnObtain<String>) {

    try {

        getString(resId).obfuscate(callback = callback)

    } catch (e: NotFoundException) {

        callback.onFailure(e)
    }
}

fun Activity.selectExternalStorageFolder(name: String, requestId: Int = DEFAULT_ACTIVITY_REQUEST) {

    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_TITLE, name)
    }

    startActivityForResult(intent, requestId)
}

fun Activity.initRegistrationWithGoogle(defaultWebClientId: Int): Int {

    val tag = "Google account :: Registration init. ::"


    val requestCode = randomInteger()
    val clientId = getString(defaultWebClientId)

    Console.log("$tag START: $clientId")

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId)
        .requestEmail()
        .build()

    val client = GoogleSignIn.getClient(this, gso)
    val account = GoogleSignIn.getLastSignedInAccount(this)

    if (account != null) {

        Console.log("$tag Account already available: ${account.email}")

        client.signOut()
    }

    Console.log("$tag No account available")

    try {

        startActivityForResult(client.signInIntent, requestCode)

    } catch (e: ActivityNotFoundException) {

        recordException(e)
    }

    Console.log("$tag END :: Req. code: $requestCode")

    return requestCode
}

fun Context.readRawTextFile(resId: Int): String {

    var line: String?
    val stringBuilder = StringBuilder()
    val inputStream = resources.openRawResource(resId)
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))

    try {

        while (bufferedReader.readLine().also { line = it } != null) {

            stringBuilder.append(line)
            stringBuilder.append('\n')
        }

    } catch (e: Throwable) {

        Console.error(e)

    } finally {

        try {

            bufferedReader.close()

        } catch (e: Throwable) {

            Console.error(e)
        }
    }

    return stringBuilder.toString().trim()
}

fun Activity.onUI(fallback: (() -> Unit)? = null, doWhat: () -> Unit) {

    runOnUiThread {

        if (isFinishing) {

            fallback?.let {

                it()
            }

        } else {

            doWhat()
        }
    }
}

fun onUiThread(doWhat: () -> Unit) {

    try {

        UI.execute { doWhat() }

    } catch (e: RejectedExecutionException) {

        recordException(e)
    }
}

@Throws(IllegalArgumentException::class)
fun getFileNameAndExtension(fileName: String): Pair<String, String> {

    if (TextUtils.isEmpty(fileName)) {

        throw IllegalArgumentException("Empty file name")
    }
    val tokens = fileName.split("\\.(?=[^.]+$)".toRegex()).toTypedArray()
    if (tokens.size < 2) {

        throw IllegalArgumentException("Could not extract file name and extension from: $fileName")
    }
    return Pair(tokens[0], tokens[1])
}

@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
fun Context.getCachedMediaFile(

    uri: Uri,
    workingDir: File = cacheDir,
    outputFileName: String = ""

): File {

    val fileName = getFileName(uri)
    if (TextUtils.isEmpty(fileName)) {

        throw IllegalArgumentException("Could not obtain file name from Uri: $uri")
    }
    fileName?.let { fName ->

        val resolver = contentResolver
        val pair = getFileNameAndExtension(fName)
        val name = if (TextUtils.isEmpty(outputFileName)) {

            pair.first
        } else {

            outputFileName
        }
        val extension = pair.second

        val outputFile = File(workingDir.absolutePath, "$name.$extension")
        outputFile.init()

        val errMsg = "Could not open input stream from: $uri"
        val inputStream =
            resolver.openInputStream(uri) ?: throw IllegalArgumentException(errMsg)
        val bufferedInputStream = BufferedInputStream(inputStream)

        var stored = 0
        val capacity = 1024
        val buffer = ByteArray(capacity)
        var read = bufferedInputStream.read(buffer)
        val outputStream = FileOutputStream(outputFile)
        val bufferedOutputStream = BufferedOutputStream(outputStream)
        while (read != -1) {

            bufferedOutputStream.write(buffer)
            stored += read
            read = bufferedInputStream.read(buffer)
        }
        if (stored == 0) {

            throw IllegalStateException("No bytes stored into: ${outputFile.absolutePath}")
        }
        Console.log("$stored bytes written into ${outputFile.absolutePath}")

        bufferedOutputStream.flush()
        bufferedOutputStream.close()
        outputStream.close()
        bufferedInputStream.close()
        inputStream.close()
        Console.log("File length ${outputFile.name}: ${outputFile.length()}")

        return outputFile
    }
    throw IllegalArgumentException("Could not obtain path from Uri: $uri")
}

@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
fun Context.writeIntoFileBuffered(

    where: File,
    what: ByteArray,
    deleteIfExist: Boolean = false

): Boolean {

    where.init(deleteIfExist)

    val inputStream = ByteArrayInputStream(what)
    val bufferedInputStream = BufferedInputStream(inputStream)

    var stored = 0
    val capacity = 1024 * 4
    val buffer = ByteArray(capacity)
    var read = bufferedInputStream.read(buffer)
    val outputStream = FileOutputStream(where)
    val bufferedOutputStream = BufferedOutputStream(outputStream)

    while (read != -1) {

        bufferedOutputStream.write(buffer)
        stored += read
        read = bufferedInputStream.read(buffer)
    }

    if (stored == 0) {

        throw IllegalStateException("No bytes stored into: ${where.absolutePath}")
    }

    Console.log("$stored bytes written into ${where.absolutePath}")

    bufferedOutputStream.flush()
    bufferedOutputStream.close()

    outputStream.flush()
    outputStream.close()

    bufferedInputStream.close()
    inputStream.close()

    Console.log("File length ${where.name}: ${where.length()}")

    return true
}

@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
fun Context.writeIntoFile(

    where: File,
    what: InputStream,
    deleteIfExist: Boolean = false

): Boolean {

    where.init(deleteIfExist)

    val bufferedInputStream = BufferedInputStream(what)

    var stored = 0
    val capacity = 1024
    val buffer = ByteArray(capacity)
    var read = bufferedInputStream.read(buffer)
    val outputStream = FileOutputStream(where)
    val bufferedOutputStream = BufferedOutputStream(outputStream)

    while (read != -1) {

        bufferedOutputStream.write(buffer)
        stored += read
        read = bufferedInputStream.read(buffer)
    }

    if (stored == 0) {

        throw IllegalStateException("No bytes stored into: ${where.absolutePath}")
    }

    Console.log("$stored bytes written into ${where.absolutePath}")

    bufferedOutputStream.flush()
    bufferedOutputStream.close()
    outputStream.close()
    bufferedInputStream.close()
    what.close()

    Console.log("File length ${where.name}: ${where.length()}")

    return true
}

@Throws(IllegalStateException::class)
fun File.init(deleteIfExist: Boolean = true) {

    Console.log("Initializing file: $absolutePath")

    if (deleteIfExist && exists()) {

        Console.warning("File already exists: $absolutePath")

        if (delete()) {

            Console.log("File deleted: $absolutePath")

        } else {

            val msg = "File could not be deleted: $absolutePath"
            throw IllegalStateException(msg)
        }
    }

    val msg = "File could not be created: $absolutePath"

    try {

        val created = createNewFile()
        val exists = exists()

        if (created && exists) {

            Console.log("File created: $absolutePath")

        } else {

            throw IllegalStateException(msg)
        }

    } catch (e: IOException) {

        Console.error(e)
        throw IllegalStateException(msg)
    }
}

fun File.appendText(text: String): Boolean {

    try {

        val mainWriter = FileWriter(this, true)
        val writer = BufferedWriter(mainWriter)

        writer.write(text)
        writer.newLine()
        writer.close()

    } catch (e: Throwable) {

        e.printStackTrace()

        return false
    }

    return true
}

fun Context.isLowEndDevice(): Boolean {

    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val lowMemory = activityManager.isLowRamDevice
    val memoryClass = activityManager.memoryClass
    val processors = Runtime.getRuntime().availableProcessors()
    return lowMemory || processors <= 4 || memoryClass <= 192
}

fun Context.toast(msg: Int, short: Boolean = false) {

    val msgString = getString(msg)
    toast(msgString, short)
}

fun Context.toast(msg: String, short: Boolean = false) {

    val length = if (short) {

        Toast.LENGTH_SHORT

    } else {

        Toast.LENGTH_LONG
    }

    Handler(Looper.getMainLooper()).post {

        Toast.makeText(applicationContext, msg, length).show()
    }
}

/** @noinspection deprecation
 */
fun Context.wakeUpScreen() {

    val tag = "Wake up screen ::"

    try {

        Console.log("$tag START")

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager?

        powerManager?.let {

            val isScreenOn = it.isInteractive

            if (isScreenOn) {

                Console.log("$tag END :: Screen is on")

            } else {

                val tag = "Sekur:WakeLock:1"

                val wl = it.newWakeLock(

                    PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    tag
                )

                wl.acquire(2000)

                val wlCpu = it.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)

                wlCpu.acquire(2000)

                Console.log("$tag END")
            }
        }

        if (powerManager == null) {

            Console.error("$tag PowerManager is null")
        }

    } catch (e: Throwable) {

        recordException(e)
        Console.error("$tag END")
    }
}

fun Activity.toast(error: Throwable) {

    toast(error, short = true, localised = true)
}

fun Activity.toast(error: Throwable, short: Boolean = false, localised: Boolean = false) {

    Console.error(error)

    val msg = if (localised) {

        error.message

    } else {

        error.localizedMessage
    }

    msg?.let {

        toast(it, short)
        return
    }

    error::class.simpleName?.let {

        toast("Error, $it", short)
        return
    }

    toast("Error", short)
}

fun Context.playNotificationSound() {

    try {

        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(this, notification)

        val aa = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        r.audioAttributes = aa
        r.play()

    } catch (e: Throwable) {

        Console.error(e)
    }
}

fun join(what: List<String>, separator: String = ", "): String {

    var result = ""

    what.forEachIndexed { index, value ->

        result += value

        if (index != what.lastIndex) {

            result += separator
        }
    }

    return result
}

fun <T> safeRemoteValue(provider: () -> T, default: T): T {

    try {

        val result = provider()
        result?.let {

            return it
        }

    } catch (e: Throwable) {

        Console.error(e)
    }

    return default
}

fun safeRemoteString(provider: () -> String): String {

    return safeRemoteValue(provider, "")
}

fun safeRemoteString(provider: () -> String, default: String = ""): String {

    return safeRemoteValue(provider, default)
}

fun safeRemoteBoolean(provider: () -> Boolean): Boolean {

    return safeRemoteValue(provider, false)
}

fun safeRemoteBoolean(provider: () -> Boolean, default: Boolean = false): Boolean {

    return safeRemoteValue(provider, default)
}

fun safeRemoteInteger(provider: () -> Int): Int {

    return safeRemoteValue(provider, 0)
}

fun safeRemoteInteger(provider: () -> Int, default: Int = 0): Int {

    return safeRemoteValue(provider, default)
}

fun safeRemoteLong(provider: () -> Long): Long {

    return safeRemoteValue(provider, 0)
}

fun safeRemoteLong(provider: () -> Long, default: Long = 0): Long {

    return safeRemoteValue(provider, default)
}

fun safeRemoteFloat(provider: () -> Float): Float {

    return safeRemoteValue(provider, 0f)
}

fun safeRemoteFloat(provider: () -> Float, default: Float = 0f): Float {

    return safeRemoteValue(provider, default)
}

fun safeRemoteDouble(provider: () -> Double): Double {

    return safeRemoteValue(provider, 0.0)
}

fun safeRemoteDouble(provider: () -> Double, default: Double = 0.0): Double {

    return safeRemoteValue(provider, default)
}

fun exec(

    onRejected: ((Throwable) -> Unit)? = { err -> recordException(err) },
    what: Runnable

) {

    exec(null, false, onRejected, what)
}

fun exec(

    tag: String? = null,
    debug: Boolean = false,

    onRejected: ((Throwable) -> Unit)? = { err -> recordException(err) },
    what: Runnable

) {

    val eTag = if (isEmpty(tag)) {

        if (debug) {

            "Exec ::"
        }

        ""

    } else {

        if (debug) {

            "$tag Exec ::"

        }

        ""
    }

    val tag = eTag

    if (debug) Console.log("$tag START")

    try {

        if (debug) Console.log("$tag Sending to main executor")

        Executor.MAIN.execute(what)

        if (debug) Console.log("$tag Sent to main executor")

    } catch (e: RejectedExecutionException) {

        if (debug) Console.error("$tag ERROR: ${e.message}")

        onRejected?.let {

            it(e)
        }

        if (onRejected == null) {

            recordException(e)
        }
    }

    if (debug) Console.log("$tag END")
}


fun exec(what: Runnable, delayInMilliseconds: Long) {

    try {

        Executor.MAIN.execute(what, delayInMilliseconds)

    } catch (e: Throwable) {

        recordException(e)
    }
}

fun exec(delayInMilliseconds: Long, what: Runnable) {

    try {

        Executor.MAIN.execute(what, delayInMilliseconds)

    } catch (e: Throwable) {

        recordException(e)
    }
}

fun exec(what: Runnable) {

    try {

        if (!Executor.MAIN.execute(what)) {

            throw IllegalStateException("Not executed")
        }

    } catch (e: Throwable) {

        recordException(e)
    }
}

@Throws(

    IllegalStateException::class
)
fun ui(what: Runnable) {

    Executor.UI.execute(what)
}

@Throws(

    RejectedExecutionException::class,
    NullPointerException::class
)
fun single(what: Runnable) {

    Executor.SINGLE.execute(what)
}

fun exec(

    callable: Callable<Boolean>,
    logTag: String = "Bool exec ::",
    executor: Execution? = null,
    debug: Boolean = false

): Boolean {

    val result = doExec(

        callable = callable,
        logTag = logTag,
        executor = executor,
        debug = debug
    )

    result?.let {

        return it
    }

    return false
}

fun <T> doExec(

    callable: Callable<T>,
    logTag: String = "Do exec ::",
    executor: Execution? = null,
    debug: Boolean = false

): T? {

    var success: T? = null
    val future: Future<T>? = null

    try {

        executor?.let {

            success = it.execute(callable)
        }

        if (executor == null) {

            success = Executor.MAIN.execute(callable)
        }

        if (debug) {

            Console.log("$logTag Callable: PRE-START")
        }

        if (debug) {

            if (success != null) {

                Console.log("$logTag Callable: RETURNED: $success")

            } else {

                Console.log("$logTag Callable: RETURNED NOTHING")
            }

            Console.log("$logTag Callable: POST-END")
        }

        return success

    } catch (e: TimeoutException) {

        Console.error(e)

        future?.cancel(true)

    } catch (e: RejectedExecutionException) {

        Console.error(e)

        future?.cancel(true)

    } catch (e: IllegalStateException) {

        Console.error(e)

        future?.cancel(true)

    } catch (e: IllegalArgumentException) {

        Console.error(e)

        future?.cancel(true)

    } catch (e: Throwable) {

        recordException(e)
    }

    return success
}

fun CountDownLatch.safeWait(timeoutInSeconds: Int = 60, tag: String = "") {

    val sTag = "Countdown latch :: Safe wait ::"

    val wTag = if (tag.isEmpty()) {

        sTag

    } else {

        "$tag $sTag"
    }

    Console.log("$wTag START")

    try {

        if (await(timeoutInSeconds.toLong(), TimeUnit.SECONDS)) {

            Console.log("$wTag END")

        } else {

            Console.error("$wTag TIMEOUT")
        }

    } catch (e: InterruptedException) {

        Console.error("$wTag ERROR: ${e.message}")

        recordException(e)
    }
}

fun yield(context: String, check: Obtain<Boolean>) {

    val tag = "YIELD :: $context ::"

    val startTime = System.currentTimeMillis()

    fun condition() = !check.obtain() && !Thread.currentThread().isInterrupted

    if (condition()) {

        Console.log("$tag START")

        while (condition()) {

            Thread.yield()
        }

        val endTime = System.currentTimeMillis() - startTime

        if (Thread.currentThread().isInterrupted) {

            Console.warning("$tag YIELDED interrupted after $endTime ms")

        } else if (endTime > 1500 && endTime < 3000) {

            Console.warning("$tag YIELDED for $endTime ms")

        } else if (endTime >= 3000) {

            Console.error("$tag YIELDED for $endTime ms")

        } else {

            Console.log("$tag END ::")
        }
    }

    // TODO: Coroutines support
}

fun <X> sync(

    context: String,
    from: String = "",
    timeout: Long = 60,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    mainThreadForbidden: Boolean = true,
    waitingFlag: AtomicBoolean? = null,
    debug: Boolean = false,
    what: (callback: OnObtain<X?>) -> Unit

): X? {

    // TODO: Coroutines support

    val tag = if (from.isEmpty()) {

        "SYNC :: $context ::"

    } else {

        "SYNC :: $context :: from '$from' ::"
    }

    val ctx = if (from.isEmpty()) {

        "SYNC.$context"

    } else {

        "SYNC.$context(from='$from')"
    }

    if (DEBUG_SYNC.get() || debug) Console.debug("$tag START")

    if (mainThreadForbidden && isOnMainThread()) {

        val e = IllegalStateException("$context executed sync on main thread")
        Console.error("$tag ${e.message}")
        recordException(e)
    }

    var result: X? = null
    val latch = CountDownLatch(1, context = ctx)

    exec(

        onRejected = { e ->

            recordException(e)

            latch.countDown()
        }

    ) {

        if (DEBUG_SYNC.get() || debug) Console.log("$tag EXECUTING")

        try {

            if (DEBUG_SYNC.get() || debug) Console.log("$tag CALLING")

            what(

                object : OnObtain<X?> {

                    override fun onCompleted(data: X?) {

                        result = data
                        if (DEBUG_SYNC.get() || debug) Console.log("$tag FINISHED")
                        latch.countDown()
                    }

                    override fun onFailure(error: Throwable) {

                        recordException(error)
                        Console.error("$tag FINISHED WITH ERROR :: Error='${error.message}'")
                        latch.countDown()
                    }
                }
            )

            if (DEBUG_SYNC.get() || debug) Console.log("$tag WAITING")

        } catch (e: Throwable) {

            Console.error("$tag FAILED :: Error='${e.message}'")
            recordException(e)
            latch.countDown()
        }
    }

    val startTime = System.currentTimeMillis()

    try {

        if (waitingFlag?.get() == true) {

            Console.warning("$tag Already waiting")

            yieldWhile {

                waitingFlag.get()
            }
        }

        waitingFlag?.set(true)

        if (latch.await(timeout, timeUnit)) {

            val endTime = System.currentTimeMillis() - startTime

            waitingFlag?.set(false)

            if (DEBUG_SYNC.get()) {

                if (endTime > 1500 && endTime < 3000) {

                    Console.warning("$tag WAITED for $endTime ms")

                } else if (endTime >= 3000) {

                    Console.warning("$tag WAITED for $endTime ms")
                }
            }

            if (DEBUG_SYNC.get() || debug) Console.debug("$tag END")

        } else {

            waitingFlag?.set(false)

            val endTime = System.currentTimeMillis() - startTime
            val e = TimeoutException("$context latch expired")
            Console.error("$tag FAILED :: Timed out after $endTime ms")
            recordException(e)
        }

    } catch (e: Throwable) {

        waitingFlag?.set(false)

        val endTime = System.currentTimeMillis() - startTime
        Console.error("$tag FAILED :: Error='${e.message}' after $endTime ms")
        recordException(e)
    }

    return result
}

private val UI_IN_SYNC = AtomicBoolean()

fun syncUI(context: String, from: String, what: kotlinx.coroutines.Runnable): Boolean {

    fun doExecute(what: kotlinx.coroutines.Runnable, callback: OnObtain<Boolean?>) {

        if (isOnMainThread()) {

            what.run()

            callback.onCompleted(true)

        } else {

            val submitted = UI.execute {

                what.run()

                callback.onCompleted(true)
            }

            if (!submitted) {

                callback.onCompleted(false)
            }
        }
    }

    val from = "syncUi(from='$from')"

    val result = sync(

        context,
        from,

        mainThreadForbidden = false,
        waitingFlag = UI_IN_SYNC,
        debug = DEBUG_UI_SYNC.get()

    ) { callback ->

        doExecute(what, callback)

    } == true

    return result
}

@Throws(IllegalArgumentException::class)
fun encodeBytes(bytes: ByteArray): String {

    val tag = "Encode bytes ::"

    Console.log("$tag START")

    @Throws(IOException::class)
    fun doEncodeBytes(bytes: ByteArray): ByteArray? {

        Console.log("$tag DO ENCODE :: START")

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var bufferedInputStream: InputStream? = null
        var base64OutputStream: OutputStream? = null

        try {

            inputStream = bytes.inputStream()
            bufferedInputStream = BufferedInputStream(inputStream)
            outputStream = ByteArrayOutputStream()
            base64OutputStream = Base64OutputStream(outputStream, Base64.NO_WRAP)

            bufferedInputStream.copyTo(base64OutputStream, 1024)

            val resultBytes = outputStream.toByteArray()

            Console.log("$tag DO ENCODE :: END")

            return resultBytes

        } catch (e: IOException) {

            Console.error(e)

        } finally {

            base64OutputStream?.close()
            outputStream?.close()
            bufferedInputStream?.close()
            inputStream?.close()
        }

        throw IOException("Encoding failure")
    }

    try {

        doEncodeBytes(bytes)?.let {

            return String(it)
        }

    } catch (e: OutOfMemoryError) {

        recordException(e)
    }

    throw IllegalArgumentException("No bytes encoded")
}

fun isEmpty(what: String?): Boolean {

    return TextUtils.isEmpty(what)
}

fun isNotEmpty(what: String?): Boolean {

    return !isEmpty(what)
}

fun List<*>.contentEquals(other: List<*>): Boolean {

    if (size == other.size) {

        forEachIndexed { index, item ->

            val otherItem = other[index]

            if (item == otherItem) {

                if (item is PropertiesHash && otherItem is PropertiesHash) {

                    if (item.propertiesHash() != otherItem.propertiesHash()) {

                        Console.log(

                            "contentEquals :: FALSE (prop. hash eq.) :: " +
                                    "${item.propertiesHash()} != ${otherItem.propertiesHash()}"
                        )

                        return false
                    }
                }

            } else {

                Console.log(

                    "contentEquals :: FALSE (equality) :: " +
                            "${item.hashCode()} != ${other[index].hashCode()}"
                )

                return false
            }
        }

    } else {

        Console.log(

            "contentEquals :: FALSE (sizes comparison) :: " +
                    "$size != ${other.size}"
        )

        return false
    }

    return true
}


fun StrictMode.VmPolicy.Builder.detectAllExpect(

    ignoredViolationPackageName: String,
    justVerbose: Boolean = true

): StrictMode.VmPolicy.Builder {

    val ex = Executors.newSingleThreadExecutor()

    return detectAll().penaltyListener(ex) {

        it.filter(ignoredViolationPackageName, justVerbose)
    }
}


private fun Violation.filter(

    ignoredViolationPackageName: String,
    justVerbose: Boolean

) {

    val violationPackageName = stackTrace[0].className

    if (violationPackageName != ignoredViolationPackageName && justVerbose) {

        Console.log(this)
    }
}

fun isOnMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

fun Throwable.toHumanReadableString(): String {

    try {

        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        printStackTrace(printWriter)

        return stringWriter.toString()

    } catch (e: Throwable) {

        recordException(e)
    }

    return ""
}

/*
* Default pattern gives Date in following format: "YYYY-MM-DD HH:MM:SS"
*/
fun Date.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {

    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}

fun <F, S> getPair(first: F, second: S): Pair<F, S> = Pair(first, second)

@Suppress("UNCHECKED_CAST")
@Throws(ClassCastException::class)
fun <F, S> getPair(map: LinkedTreeMap<String, Any>): Pair<F, S> {

    return Pair((map["first"] as String) as F, (map["second"] as String) as S)
}

@Suppress("UNCHECKED_CAST")
@Throws(ClassCastException::class)
fun <T> Any.wrapToList(): List<T> {

    return mutableListOf(this as T)
}

fun <IN, OUT> List<IN>.prepack(converter: (what: IN) -> OUT): List<OUT> {

    val items = mutableListOf<OUT>()

    forEach { item ->

        val converted = converter(item)

        items.add(converted)
    }

    return items
}

fun Context.dpToPx(dp: Float): Float {

    val density = resources?.displayMetrics?.density
    return dp * (density ?: 0f)
}
