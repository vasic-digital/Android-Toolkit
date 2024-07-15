package com.redelf.commons.test

import com.redelf.commons.data.list.HttpStringsListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.proxy.http.HttpProxies
import com.redelf.commons.proxy.http.HttpProxy
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class HttpProxiesTest : BaseTest() {

    @Test
    fun testComparison() {

        val proxy1 = "http://192.168.1.1:8080"
        val proxy2 = "http://192.168.1.1:8080"
        val proxy3 = "http://192.168.1.2:8080"

        val httpProxy1 = HttpProxy(applicationContext, proxy1)
        val httpProxy2 = HttpProxy(applicationContext, proxy2)
        val httpProxy3 = HttpProxy(applicationContext, proxy3)

        Assert.assertEquals(httpProxy1, httpProxy2)
        Assert.assertNotEquals(httpProxy1, httpProxy3)

        Assert.assertEquals(httpProxy1.hashCode(), httpProxy2.hashCode())
        Assert.assertNotEquals(httpProxy1.hashCode(), httpProxy3.hashCode())
        Assert.assertNotEquals(httpProxy2.hashCode(), httpProxy3.hashCode())
    }

    @Test
    fun testRawSourceProxies() {

        try {

            var proxies = HttpProxies(applicationContext, alive = false)
            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            proxies = HttpProxies(applicationContext, alive = true)
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

    @Test
    fun testRawSourceProxy() {

        try {

            val source = RawStringsListDataSource(applicationContext, R.raw.test_proxies_2)
            var proxies = HttpProxies(applicationContext, sources = listOf(source), alive = false)
            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.size == 1)

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

    @Test
    fun testHttpSourceProxies() {

        try {

            val sourceAddress = "https://raw.githubusercontent.com/red-elf/Android-Toolkit/main/Main/src/androidTest/res/raw/test_proxies.txt"
            val source = HttpStringsListDataSource(sourceAddress)
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