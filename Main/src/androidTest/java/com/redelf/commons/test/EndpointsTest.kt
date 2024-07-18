package com.redelf.commons.test

import com.redelf.commons.logging.Console
import com.redelf.commons.net.endpoint.http.HttpEndpoint
import com.redelf.commons.net.endpoint.http.HttpEndpoints
import org.junit.Assert
import java.util.concurrent.atomic.AtomicLong

abstract class EndpointsTest : BaseTest() {

    protected open fun getAndTestDefaultEndpoints() {

        try {

            var endpoints = HttpEndpoints(applicationContext, alive = false)
            var obtained = endpoints.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            endpoints = HttpEndpoints(applicationContext, alive = true)
            obtained = endpoints.obtain()

            Assert.assertNotNull(obtained)

            val iterator = obtained.iterator()
            val quality = AtomicLong(Long.MAX_VALUE)

            while (iterator.hasNext()) {

                val endpoint = iterator.next()

                Assert.assertNotNull(endpoint)
                Assert.assertTrue(endpoint.address.isNotBlank())
                Assert.assertTrue(endpoint.isAlive(applicationContext))

                val newQuality = endpoint.getQuality()

                Assert.assertTrue(newQuality < quality.get())

                quality.set(newQuality)

                onEndpoint(endpoint)
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }

    protected open fun onEndpoint(endpoint: HttpEndpoint) {

        Console.log("Endpoint :: $endpoint")
    }
}