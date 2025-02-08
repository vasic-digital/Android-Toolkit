package com.redelf.commons.test

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.logging.Console
import org.junit.Before
import org.junit.Test

class DataManagementTest : BaseTest() {

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testDataManagement() {

        // TODO: Implement test
    }
}