package com.redelf.commons.test

import com.redelf.commons.data.list.HttpStringsListDataSource
import com.redelf.commons.net.proxy.http.HttpProxies
import com.redelf.commons.obtain.suspendable.Obtain
import org.junit.Assert
import java.util.concurrent.atomic.AtomicLong

abstract class ProxiesTest : EndpointsTest() {

    protected open fun testHttpSourceProxies(sourceAddress: String) {

        try {

            val urlObtain = object : Obtain<String> {

                override fun obtain() = sourceAddress
            }

            val source = HttpStringsListDataSource(urlObtain)
            var proxies = HttpProxies(applicationContext, sources = listOf(source), alive = false)
            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            proxies = HttpProxies(applicationContext, sources = listOf(source), alive = true)
            obtained = proxies.obtain()

            Assert.assertNotNull(obtained)

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