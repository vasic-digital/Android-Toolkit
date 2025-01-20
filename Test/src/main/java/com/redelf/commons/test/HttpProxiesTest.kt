package com.redelf.commons.test

import com.redelf.commons.data.list.HttpStringsListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.extensions.deobfuscateString
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.net.endpoint.http.HttpEndpoint
import com.redelf.commons.net.proxy.http.HttpProxies
import com.redelf.commons.net.proxy.http.HttpProxy
import com.redelf.commons.obtain.suspendable.Obtain
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

abstract class HttpProxiesTest : ProxiesTest() {

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
    fun testCreation() {

        //        val res = com.redelf.commons.R.string.ob_proxy_local_test
        //        val resPort = com.redelf.commons.R.string.ob_proxy_local_test_port
        //        val resUsername = com.redelf.commons.R.string.ob_proxy_local_test_username
        //        val resPassword = com.redelf.commons.R.string.ob_proxy_local_test_password

        //        val host = applicationContext.deobfuscateString(res)
        //        val port = applicationContext.deobfuscateString(resPort)
        //        val username = applicationContext.deobfuscateString(resUsername)
        //        val password = applicationContext.deobfuscateString(resPassword)

        val port = "2507"
        val username = "test"
        val password = "test"
        val host = "127.0.0.1"

        Assert.assertTrue(isNotEmpty(host))
        Assert.assertTrue(isNotEmpty(port))
        Assert.assertTrue(isNotEmpty(username))
        Assert.assertTrue(isNotEmpty(password))
        Assert.assertTrue(host.endsWith(".local"))

        val proxy = "http://$username:$password@$host:$port"
        val httpProxy = HttpProxy(applicationContext, proxy)

        Assert.assertEquals(httpProxy.address, host)
        Assert.assertEquals(httpProxy.username, username)
        Assert.assertEquals(httpProxy.password, password)
        Assert.assertEquals(httpProxy.port, port.toInt())
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

    protected fun testRawSourceProxy(rawRes: Int) {

        try {

            val source = RawStringsListDataSource(applicationContext, rawRes, throwOnError = true)
            var proxies = HttpProxies(applicationContext, sources = listOf(source), alive = false)
            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())
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

        val sourceAddress = "https://raw.githubusercontent.com/red-elf/Android-Toolkit/main/" +
                "Test/src/main/res/raw/proxies.txt"

        testHttpSourceProxies(sourceAddress)
    }

    @Test
    fun testHttpDynamicSourceProxies() {

        getAndTestDefaultEndpoints()
    }

    protected fun testMixedSourceProxy(rawRes: Int) {

        try {

            val sourceRaw = RawStringsListDataSource(applicationContext, rawRes, throwOnError = true)

            val urlObtain = object : Obtain<String> {

                override fun obtain() = "https://raw.githubusercontent.com/red-elf/" +
                        "Android-Toolkit/main/Main/src/androidTest/res/raw/proxies_local.txt"
            }

            val sourceHttp = HttpStringsListDataSource(urlObtain)
            val source = listOf(sourceRaw, sourceHttp)

            var proxies = HttpProxies(applicationContext, sources = source, alive = false)
            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            proxies = HttpProxies(applicationContext, sources = source, alive = true)
            obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())
            Assert.assertTrue(obtained.size in 1..obtained.size)

            val quality = AtomicLong(-1L)
            var proxy = obtained.poll()

            while (proxy != null) {

                Assert.assertNotNull(proxy)
                Assert.assertTrue(proxy.address.isNotBlank())
                Assert.assertTrue(proxy.port > 0)
                Assert.assertTrue(proxy.isAlive(applicationContext))

                val newQuality = proxy.getQuality()
                val assertCondition = newQuality > quality.get()

                if (!assertCondition) {

                    Console.error("Proxy quality assert failure")
                }

                Assert.assertTrue(assertCondition)

                quality.set(newQuality)

                proxy = obtained.poll()
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }

    protected fun testMixedSourceProxyCombined(rawRes: Int) {

        try {

            val sourceRaw = RawStringsListDataSource(applicationContext, rawRes, throwOnError = true)

            val urlObtain = object : Obtain<String> {

                override fun obtain() = "https://raw.githubusercontent.com/red-elf/" +
                        "Android-Toolkit/main/Main/src/androidTest/res/raw/proxies_local.txt"
            }

            val sourceHttp = HttpStringsListDataSource(urlObtain)

            val source = listOf(sourceRaw, sourceHttp)

            var proxies = HttpProxies(

                applicationContext,
                sources = source,
                alive = false,
                combineSources = true
            )

            var obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

            proxies = HttpProxies(

                applicationContext,
                sources = source,
                alive = true,
                combineSources = true
            )

            obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())
            Assert.assertTrue(obtained.size in 1..obtained.size)

            val quality = AtomicLong(-1L)
            var proxy = obtained.poll()

            while (proxy != null) {

                Assert.assertNotNull(proxy)
                Assert.assertTrue(proxy.address.isNotBlank())
                Assert.assertTrue(proxy.port > 0)
                Assert.assertTrue(proxy.isAlive(applicationContext))

                val newQuality = proxy.getQuality()

                Assert.assertTrue(newQuality > quality.get())

                quality.set(newQuality)

                proxy = obtained.poll()
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }

    override fun onEndpoint(endpoint: HttpEndpoint) {
        super.onEndpoint(endpoint)

        val sourceAddress = endpoint.address

        testHttpSourceProxies(sourceAddress)
    }

    protected fun testProxy(res: Int, resPort: Int, resUsername: Int, resPassword: Int) {

        try {

            val plTest = applicationContext.deobfuscateString(res)
            val plTestPort = applicationContext.deobfuscateString(resPort)
            val plTestUsername = applicationContext.deobfuscateString(resUsername)
            val plTestPassword = applicationContext.deobfuscateString(resPassword)

            listOf(

                Pair("http://google.com:80", false),
                Pair("http://$plTestUsername:$plTestPassword@$plTest:$plTestPort", true)

            ).forEach { pair ->

                val address = pair.first
                val checkAliveAndCredentials = pair.second

                val proxy = HttpProxy(applicationContext, address)

                Assert.assertNotNull(proxy)
                Assert.assertTrue(proxy.address.isNotBlank())
                Assert.assertTrue(proxy.port > 0)

                Assert.assertTrue(proxy.ping())

                if (checkAliveAndCredentials) {

                    Assert.assertTrue(isNotEmpty(proxy.username))
                    Assert.assertTrue(isNotEmpty(proxy.password))
                    Assert.assertTrue(proxy.isAlive(applicationContext))
                }
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }
}