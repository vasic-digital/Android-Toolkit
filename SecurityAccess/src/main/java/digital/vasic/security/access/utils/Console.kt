package digital.vasic.security.access.utils

import android.util.Log

object Console {
    private const val TAG = "SecurityAccess"

    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun warning(message: String) {
        Log.w(TAG, message)
    }

    fun error(message: String) {
        Log.e(TAG, message)
    }

    fun error(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    fun error(format: String, vararg args: Any?) {
        Log.e(TAG, format.format(*args))
    }
}