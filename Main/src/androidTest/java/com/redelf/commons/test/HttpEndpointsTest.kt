package com.redelf.commons.test

import com.redelf.commons.data.list.HttpStringsListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.net.endpoint.http.HttpEndpoint
import com.redelf.commons.net.proxy.http.HttpProxies
import com.redelf.commons.net.proxy.http.HttpProxy
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class HttpEndpointsTest : BaseTest() {

    @Test
    fun testComparison() {

        val endpoint1 = "http://www.yandex.com"
        val endpoint2 = "http://www.yandex.com/"
        val endpoint3 = "http://www.yandex.com:80"
        val endpoint4 = "http://www.yandex.com:80/"
        val endpoint5 = "https://www.example.com"

        val httpEndpoint1 = HttpEndpoint(applicationContext, endpoint1)
        val httpEndpoint2 = HttpEndpoint(applicationContext, endpoint2)
        val httpEndpoint3 = HttpEndpoint(applicationContext, endpoint3)
        val httpEndpoint4 = HttpEndpoint(applicationContext, endpoint4)
        val httpEndpoint5 = HttpEndpoint(applicationContext, endpoint5)

        Assert.assertEquals(httpEndpoint1, httpEndpoint2)
        Assert.assertEquals(httpEndpoint1, httpEndpoint3)
        Assert.assertEquals(httpEndpoint1, httpEndpoint4)
        Assert.assertEquals(httpEndpoint2, httpEndpoint3)
        Assert.assertEquals(httpEndpoint2, httpEndpoint4)
        Assert.assertEquals(httpEndpoint3, httpEndpoint4)

        Assert.assertNotEquals(httpEndpoint1, httpEndpoint5)

        Assert.assertEquals(httpEndpoint1.hashCode(), httpEndpoint2.hashCode())
        Assert.assertNotEquals(httpEndpoint1.hashCode(), httpEndpoint5.hashCode())
        Assert.assertNotEquals(httpEndpoint2.hashCode(), httpEndpoint5.hashCode())
    }

//    @Test
//    fun testRawSourceProxies() {
//
//        try {
//
//            var proxies = HttpProxies(applicationContext, alive = false)
//            var obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//            Assert.assertTrue(obtained.isNotEmpty())
//
//            proxies = HttpProxies(applicationContext, alive = true)
//            obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//
//            val iterator = obtained.iterator()
//            val quality = AtomicLong(Long.MAX_VALUE)
//
//            while (iterator.hasNext()) {
//
//                val proxy = iterator.next()
//
//                Assert.assertNotNull(proxy)
//                Assert.assertTrue(proxy.address.isNotBlank())
//                Assert.assertTrue(proxy.port > 0)
//                Assert.assertTrue(proxy.isAlive(applicationContext))
//
//                val newQuality = proxy.getQuality()
//
//                Assert.assertTrue(newQuality < quality.get())
//
//                quality.set(newQuality)
//            }
//
//        } catch (e: Exception) {
//
//            Assert.fail(e.message)
//        }
//    }
//
//    @Test
//    fun testRawSourceProxy() {
//
//        try {
//
//            val source = RawStringsListDataSource(applicationContext, R.raw.proxies2)
//            var proxies = HttpProxies(applicationContext, sources = listOf(source), alive = false)
//            var obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//            Assert.assertTrue(obtained.size == 1)
//
//            proxies = HttpProxies(applicationContext, sources = listOf(source), alive = true)
//            obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//
//            val iterator = obtained.iterator()
//            val quality = AtomicLong(Long.MAX_VALUE)
//
//            while (iterator.hasNext()) {
//
//                val proxy = iterator.next()
//
//                Assert.assertNotNull(proxy)
//                Assert.assertTrue(proxy.address.isNotBlank())
//                Assert.assertTrue(proxy.port > 0)
//                Assert.assertTrue(proxy.isAlive(applicationContext))
//
//                val newQuality = proxy.getQuality()
//
//                Assert.assertTrue(newQuality < quality.get())
//
//                quality.set(newQuality)
//            }
//
//        } catch (e: Exception) {
//
//            Assert.fail(e.message)
//        }
//    }
//
//    @Test
//    fun testHttpSourceProxies() {
//
//        try {
//
//            val sourceAddress = "https://raw.githubusercontent.com/red-elf/Android-Toolkit/main/Main/src/androidTest/res/raw/proxies.txt"
//            val source = HttpStringsListDataSource(sourceAddress)
//            var proxies = HttpProxies(applicationContext, sources = listOf(source), alive = false)
//            var obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//            Assert.assertTrue(obtained.isNotEmpty())
//
//            proxies = HttpProxies(applicationContext, sources = listOf(source), alive = true)
//            obtained = proxies.obtain()
//
//            Assert.assertNotNull(obtained)
//
//            val iterator = obtained.iterator()
//            val quality = AtomicLong(Long.MAX_VALUE)
//
//            while (iterator.hasNext()) {
//
//                val proxy = iterator.next()
//
//                Assert.assertNotNull(proxy)
//                Assert.assertTrue(proxy.address.isNotBlank())
//                Assert.assertTrue(proxy.port > 0)
//                Assert.assertTrue(proxy.isAlive(applicationContext))
//
//                val newQuality = proxy.getQuality()
//
//                Assert.assertTrue(newQuality < quality.get())
//
//                quality.set(newQuality)
//            }
//
//        } catch (e: Exception) {
//
//            Assert.fail(e.message)
//        }
//    }
}