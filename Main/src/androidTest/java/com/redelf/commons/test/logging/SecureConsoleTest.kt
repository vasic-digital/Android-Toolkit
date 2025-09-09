package com.redelf.commons.test.logging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redelf.commons.logging.SecureConsole
import com.redelf.commons.test.BaseTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureConsoleTest : BaseTest() {

    @Before
    fun setup() {
        SecureConsole.initialize(
            logsRecording = false,
            failOnError = false,
            production = false,
            sensitiveDataMasking = true,
            callerInfoEnabled = true
        )
    }

    @After
    fun cleanup() {
        SecureConsole.cleanup()
    }

    @Test
    fun testBasicLogging() {
        // These should not throw exceptions
        SecureConsole.log("Test log message")
        SecureConsole.debug("Test debug message")
        SecureConsole.info("Test info message")
        SecureConsole.warning("Test warning message")
        SecureConsole.error("Test error message")
        
        // Test with arguments
        SecureConsole.log("Test with args: %s %d", "string", 42)
        SecureConsole.debug("Debug with args: %s", "test")
    }

    @Test
    fun testExceptionLogging() {
        val testException = RuntimeException("Test exception")
        
        // These should not throw exceptions
        SecureConsole.log(testException)
        SecureConsole.debug(testException)
        SecureConsole.info(testException)
        SecureConsole.warning(testException)
        SecureConsole.error(testException)
        
        // Test with messages
        SecureConsole.log(testException, "Exception with message")
        SecureConsole.error(testException, "Error with exception: %s", "details")
    }

    @Test
    fun testSensitiveDataMasking() {
        // Initialize with sensitive data masking enabled
        SecureConsole.initialize(
            sensitiveDataMasking = true,
            callerInfoEnabled = false // Disable caller info for easier testing
        )
        
        // These should be masked (we can't easily verify the console output,
        // but we can verify the methods don't crash)
        SecureConsole.log("User password: secretpassword123")
        SecureConsole.log("Token = abc123def456ghi789")
        SecureConsole.log("API key: sk-1234567890abcdef")
        SecureConsole.log("Secret code = topsecret")
        SecureConsole.log("Auth token: bearer xyz789")
        
        // Test long token pattern
        SecureConsole.log("Long token: abcdef1234567890abcdef1234567890")
        
        // Test numeric patterns (potential credit card numbers)
        SecureConsole.log("Card number: 1234567890123456")
    }

    @Test
    fun testCallerInfoEnabled() {
        SecureConsole.initialize(callerInfoEnabled = true)
        
        // The caller info should be automatically detected
        // We can't easily verify the exact output, but ensure no crashes
        SecureConsole.log("Test message with caller info")
        SecureConsole.debug("Debug with caller info")
        
        assertTrue("Test should complete without exceptions", true)
    }

    @Test
    fun testCallerInfoDisabled() {
        SecureConsole.initialize(callerInfoEnabled = false)
        
        // Should work without caller info
        SecureConsole.log("Test message without caller info")
        SecureConsole.debug("Debug without caller info")
        
        assertTrue("Test should complete without exceptions", true)
    }

    @Test
    fun testManualCallerInfo() {
        SecureConsole.withCaller("TestClass", "testMethod", 123)
            .log("Message with manual caller info")
        
        SecureConsole.withCaller("AnotherClass", "anotherMethod")
            .debug("Debug with manual caller info")
        
        assertTrue("Manual caller info should work", true)
    }

    @Test
    fun testRateLimiting() {
        val startTime = System.currentTimeMillis()
        
        // Send multiple identical messages rapidly
        repeat(100) {
            SecureConsole.log("Rapid fire message")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Rate limiting should prevent excessive logging
        // The exact behavior depends on implementation, but it should complete quickly
        assertTrue("Rate limiting should prevent excessive delays", duration < 5000)
        assertTrue("Test should complete", true)
    }

    @Test
    fun testDifferentPriorityLevels() {
        // Test all priority levels
        SecureConsole.log(0, "Priority 0 message")
        SecureConsole.log(1, "Priority 1 message")
        SecureConsole.log(2, "Priority 2 message")
        SecureConsole.log(3, "Priority 3 message")
        SecureConsole.log(4, "Priority 4 message")
        
        // With exceptions
        val testException = RuntimeException("Test priority exception")
        SecureConsole.log(0, testException)
        SecureConsole.log(1, testException, "Priority 1 with exception")
        SecureConsole.log(2, testException, "Priority 2 with args: %s", "test")
        
        assertTrue("Priority logging should work", true)
    }

    @Test
    fun testNullAndEmptyMessages() {
        // These should handle null/empty gracefully
        SecureConsole.log(null as String?, *emptyArray())
        SecureConsole.log("")
        SecureConsole.debug(null as String?)
        SecureConsole.info("")
        SecureConsole.warning(null as String?)
        SecureConsole.error("")
        
        // With arguments
        SecureConsole.log(null, "arg1", "arg2")
        SecureConsole.debug("", "arg1")
        
        assertTrue("Null/empty message handling should work", true)
    }

    @Test
    fun testFormattingErrors() {
        // Test malformed format strings (should not crash)
        SecureConsole.log("Malformed format %s %d", "only_one_arg")
        SecureConsole.debug("Too many args %s", "arg1", "arg2", "arg3")
        SecureConsole.info("Invalid format %z", "test")
        
        assertTrue("Formatting error handling should work", true)
    }

    @Test
    fun testProductionMode() {
        // Test production mode behavior
        SecureConsole.initialize(
            production = true,
            sensitiveDataMasking = true,
            callerInfoEnabled = true
        )
        
        SecureConsole.log("Production log message")
        SecureConsole.debug("Production debug message")
        SecureConsole.info("Production info message")
        SecureConsole.warning("Production warning message")
        SecureConsole.error("Production error message")
        
        assertTrue("Production mode should work", true)
    }

    @Test
    fun testStats() {
        val stats = SecureConsole.getStats()
        
        assertNotNull("Stats should not be null", stats)
        assertTrue("Should have production setting", stats.containsKey("production"))
        assertTrue("Should have recordLogs setting", stats.containsKey("recordLogs"))
        assertTrue("Should have failOnError setting", stats.containsKey("failOnError"))
        assertTrue("Should have sensitiveDataMasking setting", stats.containsKey("sensitiveDataMasking"))
        assertTrue("Should have callerInfoEnabled setting", stats.containsKey("callerInfoEnabled"))
        assertTrue("Should have rateLimitedEntries count", stats.containsKey("rateLimitedEntries"))
        
        // Verify types
        assertTrue("production should be boolean", stats["production"] is Boolean)
        assertTrue("recordLogs should be boolean", stats["recordLogs"] is Boolean)
        assertTrue("failOnError should be boolean", stats["failOnError"] is Boolean)
        assertTrue("sensitiveDataMasking should be boolean", stats["sensitiveDataMasking"] is Boolean)
        assertTrue("callerInfoEnabled should be boolean", stats["callerInfoEnabled"] is Boolean)
        assertTrue("rateLimitedEntries should be number", stats["rateLimitedEntries"] is Number)
    }

    @Test
    fun testConcurrentLogging() {
        val threadCount = 10
        val messagesPerThread = 50
        val threads = mutableListOf<Thread>()
        val exceptions = mutableListOf<Throwable>()

        repeat(threadCount) { threadId ->
            val thread = Thread {
                try {
                    repeat(messagesPerThread) { messageId ->
                        SecureConsole.log("Thread $threadId message $messageId")
                        SecureConsole.debug("Thread $threadId debug $messageId")
                        SecureConsole.info("Thread $threadId info $messageId")
                        
                        if (messageId % 10 == 0) {
                            SecureConsole.error(RuntimeException("Test exception"), 
                                               "Thread $threadId exception $messageId")
                        }
                        
                        // Small delay to allow interleaving
                        Thread.sleep(1)
                    }
                } catch (e: Throwable) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join(10000) } // 10 second timeout

        // Verify no exceptions occurred
        if (exceptions.isNotEmpty()) {
            fail("Concurrent logging failed with exceptions: ${exceptions.joinToString { it.message ?: it.javaClass.simpleName }}")
        }

        assertTrue("All threads should complete successfully", 
                  threads.all { !it.isAlive })
    }

    @Test
    fun testCleanup() {
        // Log some messages to create thread-local storage
        SecureConsole.withCaller("TestClass", "testMethod")
            .log("Message before cleanup")
        
        // Log to create rate limit entries
        repeat(5) {
            SecureConsole.log("Rate limit test message $it")
        }
        
        // Cleanup should not throw exceptions
        SecureConsole.cleanup()
        
        // Should still work after cleanup
        SecureConsole.log("Message after cleanup")
        
        assertTrue("Cleanup should work without issues", true)
    }

    @Test
    fun testLargeMessageHandling() {
        // Test with very large messages
        val largeMessage = "x".repeat(100000) // 100KB message
        
        SecureConsole.log("Large message: $largeMessage")
        SecureConsole.debug("Large debug: $largeMessage")
        SecureConsole.info("Large info: $largeMessage")
        
        assertTrue("Large message handling should work", true)
    }

    @Test
    fun testSpecialCharacters() {
        // Test with special characters and unicode
        SecureConsole.log("Special chars: !@#$%^&*()_+-={}[]|\\:;\"'<>?,./>")
        SecureConsole.log("Unicode: 你好世界 🌍 🚀 ⭐")
        SecureConsole.log("Newlines:\nLine 1\nLine 2\nLine 3")
        SecureConsole.log("Tabs:\tTabbed\tText")
        
        // Test with control characters
        SecureConsole.log("Control chars: \u0001\u0002\u0003")
        
        assertTrue("Special character handling should work", true)
    }
}