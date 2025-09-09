package com.redelf.commons.logging

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.recordException
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Secure replacement for RecordingTree with comprehensive safety measures.
 * 
 * SECURITY IMPROVEMENTS:
 * - Proper file path validation and sanitization
 * - Thread-safe file operations with proper locking
 * - Resource management with auto-closeable patterns
 * - Input validation and size limits
 * - Permission validation before file operations
 * - Stack trace sanitization to prevent information leakage
 * - Rate limiting to prevent DoS attacks
 * - Proper exception handling without information exposure
 */
class SecureRecordingTree(
    private val destination: String,
    private val production: Boolean = false,
    private val maxFileSize: Long = 50 * 1024 * 1024, // 50MB max
    private val maxLogLength: Int = 4000
) : Timber.Tree(), LogParametrized, AutoCloseable {

    companion object {
        private const val MAX_TAG_LENGTH = 23
        private const val BUFFER_SIZE = 8192
        private const val MIN_LOG_INTERVAL_MS = 1L
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
        
        // File name validation pattern - only allow safe characters
        private val SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$")
        private val PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.[\\\\/]")
        
        fun filesystemGranted(): Boolean {
            return try {
                val ctx = BaseApplication.takeContext()
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                val granted = ContextCompat.checkSelfPermission(ctx, permission)
                granted == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Validate and sanitize filename
         */
        private fun sanitizeFilename(filename: String): String {
            // Remove path traversal attempts
            var sanitized = PATH_TRAVERSAL_PATTERN.matcher(filename).replaceAll("_")
            
            // Keep only safe characters
            sanitized = sanitized.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            
            // Limit length
            if (sanitized.length > 100) {
                sanitized = sanitized.substring(0, 100)
            }
            
            return sanitized.ifEmpty { "secure_log" }
        }
    }

    private val lock = ReentrantReadWriteLock()
    private var logFile: File? = null
    private var writer: BufferedWriter? = null
    private var currentFileSize: Long = 0
    private var session: String? = null
    private val dateFormat = SimpleDateFormat("yy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
    
    // Classes to ignore when determining caller
    private val ignoredClasses = setOf(
        Timber::class.java.name,
        Console::class.java.name,
        SecureConsole::class.java.name,
        Timber.Tree::class.java.name,
        Timber.Forest::class.java.name,
        SecureRecordingTree::class.java.name,
        Timber.DebugTree::class.java.name
    )
    
    // Rate limiting
    private var lastLogTime = 0L
    
    init {
        if (!isValidDestination(destination)) {
            throw IllegalArgumentException("Invalid destination name: $destination")
        }
    }
    
    private fun isValidDestination(dest: String): Boolean {
        return dest.isNotEmpty() && 
               dest.length <= 50 && 
               SAFE_FILENAME_PATTERN.matcher(dest).matches()
    }

    fun hello() {
        try {
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = format.format(calendar.time)
            
            writeSecureLog("LOG_START", "Session started at: $formattedDate")
        } catch (e: Exception) {
            recordException(e)
        }
    }

    override fun logParametrized(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (production) {
            val safeTag = sanitizeTag(tag)
            val safeMessage = sanitizeMessage(message)
            
            writeSecureLog("$safeTag::P$priority", safeMessage)
            
            t?.let { throwable ->
                val sanitizedStackTrace = sanitizeStackTrace(throwable)
                writeSecureLog(safeTag, sanitizedStackTrace)
            }
            return
        }

        log(priority, tag, message, t)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val safeTag = sanitizeTag(tag)
            val safeMessage = sanitizeMessage(message)
            
            if (safeMessage.length < maxLogLength) {
                logSingle(priority, safeTag, safeMessage)
                return
            }

            // Split long messages safely
            logLongMessage(priority, safeTag, safeMessage)
            
        } catch (e: Exception) {
            recordException(e)
        }
    }
    
    private fun logSingle(priority: Int, tag: String?, message: String) {
        if (priority == Log.ASSERT) {
            writeSecureLog(tag, message)
            Log.wtf(tag, message)
        } else {
            writeSecureLog(tag, message)
            Log.println(priority, tag, message)
        }
    }
    
    private fun logLongMessage(priority: Int, tag: String?, message: String) {
        var i = 0
        val length = message.length
        
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            
            do {
                val end = (i + maxLogLength).coerceAtMost(newline)
                val part = message.substring(i, end)
                logSingle(priority, tag, part)
                i = end
            } while (i < newline)
            i++
        }
    }
    
    private fun sanitizeTag(tag: String?): String? {
        if (tag.isNullOrEmpty()) return null
        
        // Remove potentially sensitive information from tags
        var sanitized = tag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        
        if (sanitized.length > MAX_TAG_LENGTH) {
            sanitized = sanitized.substring(0, MAX_TAG_LENGTH)
        }
        
        return sanitized.ifEmpty { "SECURE" }
    }
    
    private fun sanitizeMessage(message: String): String {
        if (message.isEmpty()) return message
        
        // Remove potential sensitive data patterns
        var sanitized = message
        
        // Sanitize common sensitive patterns
        sanitized = sanitized.replace(Regex("password\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE), "password=[REDACTED]")
        sanitized = sanitized.replace(Regex("token\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE), "token=[REDACTED]")
        sanitized = sanitized.replace(Regex("key\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE), "key=[REDACTED]")
        sanitized = sanitized.replace(Regex("secret\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE), "secret=[REDACTED]")
        
        // Limit message length
        if (sanitized.length > 10000) {
            sanitized = sanitized.substring(0, 10000) + "...[TRUNCATED]"
        }
        
        return sanitized
    }
    
    private fun sanitizeStackTrace(throwable: Throwable): String {
        return try {
            val stackTrace = throwable.stackTraceToString()
            // Remove potentially sensitive paths and information
            stackTrace
                .replace(Regex("/data/[^\\s]+", RegexOption.IGNORE_CASE), "/data/[REDACTED]")
                .replace(Regex("file://[^\\s]+", RegexOption.IGNORE_CASE), "file://[REDACTED]")
                .replace(Regex("http[s]?://[^\\s]+", RegexOption.IGNORE_CASE), "http://[REDACTED]")
                .take(5000) // Limit stack trace size
        } catch (e: Exception) {
            "Stack trace sanitization error: ${e.javaClass.simpleName}"
        }
    }

    private fun writeSecureLog(tag: String?, message: String) {
        // Rate limiting
        val now = System.currentTimeMillis()
        if (now - lastLogTime < MIN_LOG_INTERVAL_MS) {
            return
        }
        lastLogTime = now
        
        if (!filesystemGranted()) return

        lock.write {
            try {
                ensureLogFileExists()
                
                val logFile = this.logFile ?: return
                val writer = this.writer ?: return
                
                // Check file size limit
                if (currentFileSize > maxFileSize) {
                    rotateLogFile()
                }
                
                val timestamp = dateFormat.format(Calendar.getInstance().time)
                val tagPrefix = if (!tag.isNullOrEmpty()) "$tag :: " else ""
                val logLine = "$timestamp :: $tagPrefix$message\n"
                
                writer.write(logLine)
                writer.flush() // Ensure data is written
                
                currentFileSize += logLine.length
                
            } catch (e: IOException) {
                recordException(e)
                // Try to recreate file on next log
                closeWriter()
                logFile = null
            } catch (e: Exception) {
                recordException(e)
            }
        }
    }
    
    private fun ensureLogFileExists() {
        if (logFile != null && writer != null) return
        
        try {
            if (session == null) {
                val sessionFormat = SimpleDateFormat("HH-mm-ss", Locale.getDefault())
                session = sessionFormat.format(Calendar.getInstance().time)
            }
            
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)
            
            val sanitizedDestination = sanitizeFilename(destination)
            val sanitizedSession = sanitizeFilename(session ?: "default")
            val fileName = "$formattedDate-$sanitizedDestination-$sanitizedSession.txt"
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadsDir.exists()) {
                throw IOException("Downloads directory not available")
            }
            
            val newLogFile = File(downloadsDir, fileName)
            
            // Validate the final path
            val canonicalPath = newLogFile.canonicalPath
            val expectedDir = downloadsDir.canonicalPath
            
            if (!canonicalPath.startsWith(expectedDir)) {
                throw IOException("Invalid file path detected")
            }
            
            if (!newLogFile.exists()) {
                if (!newLogFile.createNewFile()) {
                    throw IOException("Could not create log file")
                }
            }
            
            logFile = newLogFile
            writer = BufferedWriter(FileWriter(newLogFile, true), BUFFER_SIZE)
            currentFileSize = newLogFile.length()
            
        } catch (e: Exception) {
            recordException(e)
            throw e
        }
    }
    
    private fun rotateLogFile() {
        try {
            closeWriter()
            session = null // Force new session for rotated file
            ensureLogFileExists()
        } catch (e: Exception) {
            recordException(e)
        }
    }
    
    private fun closeWriter() {
        try {
            writer?.flush()
            writer?.close()
            writer = null
        } catch (e: Exception) {
            recordException(e)
        }
    }
    
    override fun close() {
        lock.write {
            try {
                writeSecureLog("LOG_END", "Session ended")
                closeWriter()
                logFile = null
                session = null
                currentFileSize = 0
            } catch (e: Exception) {
                recordException(e)
            }
        }
    }
    
    /**
     * Get current log file size
     */
    fun getLogFileSize(): Long {
        return lock.read { currentFileSize }
    }
    
    /**
     * Get log file path (for monitoring)
     */
    fun getLogFilePath(): String? {
        return lock.read { logFile?.absolutePath }
    }
}