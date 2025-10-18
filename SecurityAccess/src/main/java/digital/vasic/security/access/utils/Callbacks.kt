package digital.vasic.security.access.utils

class Callbacks<T>(
    private val identifier: String
) {
    private val callbacks = mutableListOf<T>()

    fun register(callback: T) {
        synchronized(callbacks) {
            callbacks.add(callback)
        }
    }

    fun unregister(callback: T) {
        synchronized(callbacks) {
            callbacks.remove(callback)
        }
    }

    fun doOnAll(operation: CallbackOperation<T>, operationName: String) {
        val callbacksCopy = synchronized(callbacks) {
            callbacks.toList()
        }

        callbacksCopy.forEach { callback ->
            try {
                operation.perform(callback)
            } catch (e: Exception) {
                Console.error("Error in $operationName for $identifier: ${e.message}")
            }
        }
    }

    fun isEmpty(): Boolean {
        return synchronized(callbacks) {
            callbacks.isEmpty()
        }
    }

    fun size(): Int {
        return synchronized(callbacks) {
            callbacks.size
        }
    }
}