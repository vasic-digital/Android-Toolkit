/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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