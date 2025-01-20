package com.redelf.commons.test

import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.net.endpoint.http.HttpEndpoint
import com.redelf.commons.net.endpoint.http.HttpEndpoints
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class HttpEndpointsTest : EndpointsTest() {

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

    @Test
    fun testDefaultRawSourceEndpoint() = getAndTestDefaultEndpoints()


    @Test
    fun testRawSourceEndpoint() {

        try {

            val source = RawStringsListDataSource(

                applicationContext,
                com.redelf.commons.R.raw.proxy_endpoints
            )

            var endpoints =
                HttpEndpoints(applicationContext, sources = listOf(source), alive = false)

            var obtained = endpoints.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.size == 1)

            endpoints = HttpEndpoints(applicationContext, sources = listOf(source), alive = true)
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

    @Test
    fun testHttpSourceProxies() {

        // FIXME:
//        try {
//
//            val urlObtain = object : Obtain<String> {
//
//                override fun obtain() = "https://raw.githubusercontent.com/proxifly/" +
//                        "free-proxy-list/main/proxies/protocols/http/data.txt"
//            }
//
//            val source = HttpStringsListDataSource(urlObtain)
//
//            var endpoints =
//                HttpEndpoints(applicationContext, sources = listOf(source), alive = false)
//
//            var obtained = endpoints.obtain()
//
//            Assert.assertNotNull(obtained)
//            Assert.assertTrue(obtained.isNotEmpty())
//
//            endpoints = HttpEndpoints(applicationContext, sources = listOf(source), alive = true)
//            obtained = endpoints.obtain()
//
//            Assert.assertNotNull(obtained)
//
//            val iterator = obtained.iterator()
//            val quality = AtomicLong(Long.MAX_VALUE)
//
//            while (iterator.hasNext()) {
//
//                val endpoint = iterator.next()
//
//                Assert.assertNotNull(endpoint)
//                Assert.assertTrue(endpoint.address.isNotBlank())
//                Assert.assertTrue(endpoint.isAlive(applicationContext))
//
//                val newQuality = endpoint.getQuality()
//
//                Assert.assertTrue(newQuality < quality.get())
//
//                quality.set(newQuality)
//
//                onEndpoint(endpoint)
//            }
//
//        } catch (e: Exception) {
//
//            Assert.fail(e.message)
//        }
    }
}