package com.redelf.commons.test

import com.redelf.commons.firebase.FirebaseConfigurationManager

import org.junit.Assert
import org.junit.Test

abstract class FirebaseConfigurationManagerBaseTest : ManagersDependantTest() {

    override val managers = listOf(FirebaseConfigurationManager)

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