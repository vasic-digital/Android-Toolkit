package com.redelf.access

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redelf.access.implementation.AccessActivity
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class AccessTest : BaseTest() {

    @Mock
    private lateinit var mockAccessActivity: AccessActivity

    @Mock
    private lateinit var mockAccessMethod: AccessMethod

    init {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testAccessBuilderAddAccessMethod() {
        val builder = AccessBuilder()
        val method = Mockito.mock(AccessMethod::class.java)
        val result = builder.addAccessMethod(method)
        Assert.assertEquals("Builder should return itself", builder, result)
        Assert.assertTrue("Methods should contain the added method", builder.methods.contains(method))
    }



    @Test
    fun testAccessBuilderBuild() {
        val builder = AccessBuilder()
        val access = builder.build()
        Assert.assertNotNull("Access should be built", access)
    }

    // More tests for Access class would require mocking the activity and methods,
    // which is complex. For now, basic builder tests.
}