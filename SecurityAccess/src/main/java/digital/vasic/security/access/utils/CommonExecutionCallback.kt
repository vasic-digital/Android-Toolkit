package digital.vasic.security.access.utils

interface CommonExecutionCallback {
    fun onExecution(success: Boolean, calledFrom: String)
}