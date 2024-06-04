package com.redelf.commons.logging

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import com.redelf.commons.extensions.appendText
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class RecordingTree(private val destination: String) : Timber.Tree() {

    private var file: File? = null
    private var session: String? = null
    private val cal = Calendar.getInstance()
    private val fmt = SimpleDateFormat("yy-MM-dd-h-m-s-S", Locale.getDefault())

    private val fqcnIgnore = listOf(

        Timber::class.java.name,
        com.redelf.commons.logging.Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        RecordingTree::class.java.name
    )

    @get:JvmSynthetic // Hide from public API.
    internal val explicitTag = ThreadLocal<String>()

    @get:JvmSynthetic // Hide from public API.
    internal val initTag: String?
        get() {
            val tag = explicitTag.get()
            if (tag != null) {
                explicitTag.remove()
            }
            return tag
        }

    val tag: String
        get() = initTag ?: Throwable().stackTrace
            .first { it.className !in fqcnIgnore }
            .let(::createStackElementTag)

    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        // Tag length limit was removed in API 26.
        return if (tag.length <= MAX_TAG_LENGTH) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (message.length < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                writeLog(tag, message)
                Log.wtf(tag, message)
            } else {
                writeLog(tag, message)
                Log.println(priority, tag, message)
            }
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                if (priority == Log.ASSERT) {
                    writeLog(tag, part)
                    Log.wtf(tag, part)
                } else {
                    writeLog(tag, part)
                    Log.println(priority, tag, part)
                }
                i = end
            } while (i < newline)
            i++
        }
    }

    @SuppressLint("LogNotTimber")
    private fun writeLog(tag: String?, logs: String) {

        val datetime = fmt.format(cal.time)

        if (isEmpty(session)) {

            val fmt2 = SimpleDateFormat("h-m-s-S", Locale.getDefault())
            session = fmt2.format(cal.time)
        }

        if (file == null) {

            val calendar = Calendar.getInstance()
            val dir = Environment.DIRECTORY_DOWNLOADS
            val format = SimpleDateFormat("yy-MM-dd", Locale.getDefault())
            val formattedDate = format.format(calendar.time)
            val fileName = "$formattedDate-$destination-$session.txt"
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(dir)

            file = File(downloadsFolder, fileName)

            if (file?.exists() != true && file?.createNewFile() != true) {

                Timber.e("No logs gathering file crated at: ${file?.absolutePath}")
            }
        }

        val tagVal = if (isNotEmpty(tag)) {

            "$tag :: "

        } else {

            ""
        }

        if (file?.appendText("$datetime :: $tagVal$logs") != true) {

            Timber.e("Failed to append text into: ${file?.absolutePath}")
        }
    }

    companion object {

        private const val MAX_TAG_LENGTH = 23
        private const val MAX_LOG_LENGTH = 4000
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
}