package com.redelf.commons.execution

/**
 * @deprecated This class has security vulnerabilities:
 * - No delays between retry attempts causing tight CPU loops and DoS potential
 * - No timeout protection for individual attempts
 * - No exponential backoff or jitter leading to system overload
 * - Can overwhelm target systems with rapid retry attempts
 * - No maximum limits or safeguards against infinite resource consumption
 * 
 * Use SecureRetrying instead which provides:
 * - Exponential backoff with jitter to prevent system overload
 * - Timeout protection for each retry attempt
 * - Configurable maximum retry limits
 * - Performance metrics and monitoring
 * - Circuit breaker pattern support
 * 
 * @see SecureRetrying
 */
@Deprecated(
    message = "Security vulnerability: tight retry loops with no delays can cause DoS. Use SecureRetrying instead.",
    replaceWith = ReplaceWith("SecureRetrying.create(maxRetries = count)", "com.redelf.commons.execution.SecureRetrying"),
    level = DeprecationLevel.WARNING
)
class Retrying(private val count: Int = 5) {

    fun execute(operation: () -> Boolean): Int {

        var counter = 0
        while (!operation() && counter < count) {

            counter++
        }
        return counter
    }
}