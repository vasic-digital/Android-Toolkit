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


import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class DozeAwareRetryInterceptor(

    private val retryDelays: List<Long> = listOf(2000L, 2000L, 2000L)

) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        var lastException: Exception? = null

        for (attempt in 0..retryDelays.size) {

            var response: Response? = null

            try {

                response = chain.proceed(chain.request())

                if (response.isSuccessful) {

                    return response
                }

                // Close unsuccessful responses
                response.close()

                throw IOException("HTTP ${response.code} - ${response.message}")

            } catch (e: Exception) {

                lastException = e
                response?.close()

                if (attempt == retryDelays.size) {

                    break // No more retries
                }

                // Wait before retry (important for Doze)
                val delay = retryDelays[attempt]

                try {

                    Thread.sleep(delay)

                } catch (ie: InterruptedException) {

                    Thread.currentThread().interrupt()

                    throw IOException("Retry interrupted", ie)
                }
            }
        }

        throw lastException ?: IOException("Network request failed")
    }
}