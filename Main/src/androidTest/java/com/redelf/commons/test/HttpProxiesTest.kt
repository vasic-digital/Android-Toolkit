package com.redelf.commons.test

import com.redelf.commons.proxy.http.HttpProxies
import org.junit.Assert
import org.junit.Test

class HttpProxiesTest : BaseTest() {

    @Test
    fun testProxies() {

        try {

            val proxies = HttpProxies(applicationContext)
            val obtained = proxies.obtain()

            Assert.assertNotNull(obtained)
            Assert.assertTrue(obtained.isNotEmpty())

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }
}