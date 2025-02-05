package com.redelf.commons.test

import org.junit.Assert
import org.junit.Test

import io.grpc.InternalGlobalInterceptors

class ClassesAccessTest {

    @Test
    fun testClassesAccess() {

        val igi: InternalGlobalInterceptors? = null

        Assert.assertNull(igi)
    }
}