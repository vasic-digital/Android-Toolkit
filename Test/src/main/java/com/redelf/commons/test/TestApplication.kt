package com.redelf.commons.test

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.management.DataManagement

open class TestApplication : BaseApplication() {

    companion object : ContextAvailability<TestApplication> {

        private lateinit var instance: TestApplication

        override fun takeContext() = instance
    }

    override val firebaseEnabled = false
    override val managers = mutableListOf<List<DataManagement<*>>>()

    override fun takeSalt() = "test"

    override fun onDoCreate() = Unit

    override fun isProduction() = false

    override fun onCreate() {

        instance = this

        super.onCreate()
    }

    override fun takeContext(): BaseApplication {

        return instance
    }
}