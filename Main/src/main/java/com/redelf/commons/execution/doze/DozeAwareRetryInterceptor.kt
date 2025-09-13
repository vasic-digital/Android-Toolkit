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