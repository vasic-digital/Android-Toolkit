package com.redelf.commons.test

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.authentification.Credentials
import com.redelf.commons.logging.Console
import com.redelf.commons.management.Management
import com.redelf.commons.management.managers.ManagersInitializer
import org.junit.Assert
import org.junit.Before
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class ManagersDependentTest : BaseTest() {

    protected open val managers: List<Management> = listOf()

    /**
     * Credentials set to be used for testing
     * @return List of pairs: credentials to be used for testing vs expected auth. result
     */
    open val credentialsSet: List<Pair<Credentials, Boolean>> = listOf()

    protected open fun setup() {

        Console.initialize(failOnError = true)

        Console.log("Timber initialized: $this")

        BaseApplication.DEBUG.set(true)

        setupStorage()
        setupManagers()
    }

    private fun setupStorage() {

        Console.log("Timber initialized: $this")
    }

    private fun setupManagers() {

        val registered = AtomicInteger()
        val setupSuccess = AtomicBoolean()
        val mainLatch = CountDownLatch(1)
        val latch = CountDownLatch(managers.size)

        val managersInitializerCallback = object : ManagersInitializer.InitializationCallback {

            override fun onInitialization(

                manager: Management,
                success: Boolean,
                error: Throwable?

            ) {

                Assert.assertTrue(success)
                Assert.assertNull(error)

                if (success) {

                    registered.incrementAndGet()
                }

                latch.countDown()
            }

            override fun onInitialization(success: Boolean, error: Throwable?) {

                setupSuccess.set(success)

                mainLatch.countDown()
            }
        }

        ManagersInitializer().initializeManagers(

            managers,
            managersInitializerCallback,
            context = TestApplication.takeContext(),
        )

        latch.await()
        mainLatch.await()

        Assert.assertTrue(setupSuccess.get())
        Assert.assertEquals(managers.size, registered.get())
    }

    @Before
    fun doSetup() = setup()
}