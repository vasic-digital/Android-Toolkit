package digital.vasic.security.access.utils

interface CallbackOperation<T> {
    fun perform(callback: T)
}