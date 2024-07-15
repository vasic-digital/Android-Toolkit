package com.redelf.commons.test

import com.redelf.commons.proxy.http.HttpProxies
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class HttpProxiesTest : BaseTest() {

    @Test
    fun testProxies() {

        try {

            val proxies = HttpProxies(applicationContext)
            val obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            val iterator = obtained.iterator()
            val quality = AtomicLong(Long.MAX_VALUE)

            while (iterator.hasNext()) {

                val proxy = iterator.next()

                Assert.assertNotNull(proxy)
                Assert.assertTrue(proxy.address.isNotBlank())
                Assert.assertTrue(proxy.port > 0)
                Assert.assertTrue(proxy.isAlive(applicationContext))

                val newQuality = proxy.getQuality()

                Assert.assertTrue(newQuality < quality.get())

                quality.set(newQuality)
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }
}