package com.redelf.commons.test.execution

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for secure execution package.
 * 
 * This suite includes all security-focused tests for:
 * - SecureTaskExecutor: Thread pool management with security limits
 * - SecureRetrying: Retry logic with DoS protection
 * - Integration tests: Testing components working together
 * - Backward compatibility: Testing deprecated classes still work
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    SecureTaskExecutorTest::class,
    SecureRetryingTest::class,
    SecureExecutionIntegrationTest::class
)
class SecureExecutionTestSuite {
    // Test suite for secure execution package
    // Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.redelf.commons.test.execution.SecureExecutionTestSuite
}