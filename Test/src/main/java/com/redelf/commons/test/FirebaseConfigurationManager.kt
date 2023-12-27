package com.redelf.commons.test

import com.google.firebase.FirebaseApp
import com.redelf.commons.firebase.FirebaseConfigurationManager

import org.junit.Assert

abstract class FirebaseConfigurationManagerBaseTest : ManagersDependantTest() {

    override val managers = listOf(FirebaseConfigurationManager)

    override fun setup() {

        FirebaseApp.initializeApp(testContext)

        super.setup()
    }

    fun getData(): Any? {

        try {

            val data = FirebaseConfigurationManager.obtain()

            Assert.assertNotNull(data)

            return data

        } catch (e: Exception) {

            Assert.fail(e.message)
        }

        return null
    }
}