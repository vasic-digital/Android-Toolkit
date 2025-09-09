package com.redelf.commons.logging

import com.redelf.commons.extensions.recordException
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Secure replacement for Console class with comprehensive safety measures.
 * 
 * SECURITY IMPROVEMENTS:
 * - Automatic caller class and method detection
 * - Sanitized logging to prevent information leakage
 * - Thread-safe operations with proper synchronization
 * - Memory leak prevention with proper ThreadLocal cleanup
 * - Input validation and sanitization
 * - Configurable sensitive data filtering
 * - Stack trace analysis protection
 * - Rate limiting for DoS prevention
 */
object SecureConsole {

    // Security settings
    private val production = AtomicBoolean(false)
    private val recordLogs = AtomicBoolean(false)
    private val failOnError = AtomicBoolean(false)
    private val sensitiveDataMasking = AtomicBoolean(true)
    private val callerInfoEnabled = AtomicBoolean(true)
    
    // Thread-safe storage
    private val rateLimitMap = ConcurrentHashMap<String, Long>()
    private const val RATE_LIMIT_MS = 100L // Minimum time between identical logs
    
    // Sensitive data patterns
    private val sensitivePatterns = listOf(
        Pattern.compile("password\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("token\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("key\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("secret\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("auth\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b[A-Za-z0-9]{32,}\\b"), // Long tokens/hashes
        Pattern.compile("\\b[0-9]{4,16}\\b") // Credit card numbers, etc.
    )
    
    // Classes to ignore when finding caller
    private val ignoredClasses = setOf(
        SecureConsole::class.java.name,
        Console::class.java.name,
        Timber::class.java.name,
        Timber.Tree::class.java.name,
        Timber.Forest::class.java.name,
        Timber.DebugTree::class.java.name
    )
    
    // ThreadLocal with auto-cleanup
    private val callerInfoCache = object : ThreadLocal<CallerInfo?>() {
        override fun remove() {
            try {
                super.remove()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    data class CallerInfo(
        val className: String,
        val methodName: String,
        val lineNumber: Int,
        val fileName: String?
    )
    
    @JvmStatic
    fun initialize(
        logsRecording: Boolean = false,
        failOnError: Boolean = false,
        production: Boolean = false,
        sensitiveDataMasking: Boolean = true,
        callerInfoEnabled: Boolean = true
    ) {
        this.recordLogs.set(logsRecording)
        this.failOnError.set(failOnError)
        this.production.set(production)
        this.sensitiveDataMasking.set(sensitiveDataMasking)
        this.callerInfoEnabled.set(callerInfoEnabled)
        
        // Use original Console for backwards compatibility
        Console.initialize(logsRecording, failOnError, production)
        
        log("SecureConsole initialized", 
            "recording=$logsRecording", 
            "production=$production",
            "sensitiveDataMasking=$sensitiveDataMasking",
            "callerInfo=$callerInfoEnabled")
    }
    
    /**
     * Get caller information from stack trace
     */
    private fun getCallerInfo(): CallerInfo? {
        if (!callerInfoEnabled.get()) return null
        
        // Check cache first
        val cached = callerInfoCache.get()
        if (cached != null) {
            callerInfoCache.remove() // Clear after use
            return cached
        }
        
        try {
            val stackTrace = Thread.currentThread().stackTrace
            
            // Find first non-ignored class in stack trace
            val callerElement = stackTrace.firstOrNull { element ->
                element.className !in ignoredClasses &&
                !element.className.startsWith("java.") &&
                !element.className.startsWith("android.") &&
                !element.className.startsWith("kotlin.") &&
                element.methodName != "getStackTrace"
            }
            
            return callerElement?.let { element ->
                CallerInfo(
                    className = element.className.substringAfterLast('.'),
                    methodName = element.methodName,
                    lineNumber = element.lineNumber,
                    fileName = element.fileName
                )
            }
        } catch (e: Exception) {
            // Don't log errors in logging system to prevent recursion
            recordException(e)
            return null
        }
    }
    
    /**
     * Sanitize message to remove sensitive information
     */
    private fun sanitizeMessage(message: String?): String {
        if (!sensitiveDataMasking.get() || message.isNullOrBlank()) {
            return message ?: ""
        }
        
        var sanitized = message ?: ""
        sensitivePatterns.forEach { pattern ->
            try {
                sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]")
            } catch (e: Exception) {
                // Pattern matching errors shouldn't break logging
            }
        }
        
        return sanitized
    }
    
    /**
     * Format message with caller information
     */
    private fun formatMessage(message: String?, vararg args: Any?): String {
        val sanitizedMessage = sanitizeMessage(message ?: "")
        val callerInfo = getCallerInfo()
        
        val formattedMessage = if (args.isNotEmpty()) {
            try {
                String.format(sanitizedMessage, *args)
            } catch (e: Exception) {
                // Fallback if formatting fails
                "$sanitizedMessage :: Args=${args.joinToString(", ") { it.toString() }}"
            }
        } else {
            sanitizedMessage
        }
        
        return if (callerInfo != null && callerInfoEnabled.get()) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] $formattedMessage"
        } else {
            formattedMessage
        }
    }
    
    /**
     * Check rate limiting to prevent spam
     */
    private fun isRateLimited(message: String): Boolean {
        val now = System.currentTimeMillis()
        val key = message.take(50) // Use first 50 chars as key
        
        val lastLog = rateLimitMap[key]
        if (lastLog != null && (now - lastLog) < RATE_LIMIT_MS) {
            return true
        }
        
        rateLimitMap[key] = now
        
        // Clean old entries to prevent memory leaks
        if (rateLimitMap.size > 1000) {
            val cutoff = now - (RATE_LIMIT_MS * 100) // Keep last 100 intervals
            rateLimitMap.entries.removeIf { (_, time) -> time < cutoff }
        }
        
        return false
    }
    
    @JvmStatic
    fun log(message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.log(formattedMessage)
    }
    
    @JvmStatic
    fun log(t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.log(t, formattedMessage)
    }
    
    @JvmStatic
    fun log(t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.log(t, "${prefix}Exception occurred")
    }
    
    @JvmStatic
    fun debug(message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.debug(formattedMessage)
    }
    
    @JvmStatic
    fun debug(t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.debug(t, formattedMessage)
    }
    
    @JvmStatic
    fun debug(t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.debug(t, "${prefix}Debug exception")
    }
    
    @JvmStatic
    fun info(message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.info(formattedMessage)
    }
    
    @JvmStatic
    fun info(t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.info(t, formattedMessage)
    }
    
    @JvmStatic
    fun info(t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.info(t, "${prefix}Info exception")
    }
    
    @JvmStatic
    fun warning(message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.warning(formattedMessage)
    }
    
    @JvmStatic
    fun warning(t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.warning(t, formattedMessage)
    }
    
    @JvmStatic
    fun warning(t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.warning(t, "${prefix}Warning exception")
    }
    
    @JvmStatic
    fun error(message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        // Don't rate limit errors - they're important
        Console.error(formattedMessage)
    }
    
    @JvmStatic
    fun error(t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        Console.error(t, formattedMessage)
    }
    
    @JvmStatic
    fun error(t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.error(t, "${prefix}Error exception")
    }
    
    @JvmStatic
    fun log(priority: Int, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.log(priority, formattedMessage)
    }
    
    @JvmStatic
    fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
        val formattedMessage = formatMessage(message ?: "", *args)
        
        if (isRateLimited(formattedMessage)) return
        
        Console.log(priority, t, formattedMessage)
    }
    
    @JvmStatic
    fun log(priority: Int, t: Throwable?) {
        val callerInfo = getCallerInfo()
        val prefix = if (callerInfo != null) {
            "[${callerInfo.className}.${callerInfo.methodName}:${callerInfo.lineNumber}] "
        } else {
            ""
        }
        
        Console.log(priority, t, "${prefix}Priority $priority exception")
    }
    
    /**
     * Clean up thread-local storage
     */
    @JvmStatic
    fun cleanup() {
        try {
            callerInfoCache.remove()
            rateLimitMap.clear()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Set caller information manually (for special cases)
     */
    @JvmStatic
    fun withCaller(className: String, methodName: String, lineNumber: Int = 0): SecureConsole {
        callerInfoCache.set(CallerInfo(className, methodName, lineNumber, null))
        return this
    }
    
    /**
     * Get logging statistics
     */
    @JvmStatic
    fun getStats(): Map<String, Any> {
        return mapOf(
            "production" to production.get(),
            "recordLogs" to recordLogs.get(),
            "failOnError" to failOnError.get(),
            "sensitiveDataMasking" to sensitiveDataMasking.get(),
            "callerInfoEnabled" to callerInfoEnabled.get(),
            "rateLimitedEntries" to rateLimitMap.size
        )
    }
}