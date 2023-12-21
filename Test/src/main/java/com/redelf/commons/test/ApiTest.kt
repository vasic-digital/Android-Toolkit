package com.redelf.commons.test

import com.redelf.commons.Credentials
import com.redelf.commons.application.ManagersInitializer
import com.redelf.commons.management.Management
import com.redelf.commons.persistance.EncryptedPersistence

import org.junit.Assert
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

abstract class ApiTest : BaseTest() {

    private lateinit var persistence: EncryptedPersistence

    protected open val managers: List<Management> = listOf()

    /**
     * Credentials set to be used for testing
     * @return List of pairs: credentials to be used for testing vs expected auth. result
     * */
    abstract val credentialsSet: List<Pair<Credentials, Boolean>>

    protected open fun setup() {

        Timber.plant(Timber.DebugTree())
        Timber.v("Timber initialized: $this")

        setupStorage()
        setupManagers()
    }

    private fun setupStorage() {

        Timber.plant(Timber.DebugTree())
        Timber.v("Timber initialized: $this")

        val keySalt = "test.${System.currentTimeMillis()}"
        val storageTag = "test.${System.currentTimeMillis()}"

        persistence = EncryptedPersistence(

            ctx = applicationContext,
            keySalt = keySalt,
            storageTag = storageTag
        )
    }

    private fun setupManagers() {

        val failed = AtomicInteger()
        val registered = AtomicInteger()
        val latch = CountDownLatch(managers.size)

        val managersInitializerCallback = object : ManagersInitializer.InitializationCallback {

            override fun onInitialization(success: Boolean, error: Throwable?) {

                if (success) {

                    registered.incrementAndGet()

                    Timber.i("Application: Initialized")

                } else {

                    failed.incrementAndGet()

                    error?.let {

                        Assert.fail(it.message)
                    }
                }

                latch.countDown()
            }
        }

        ManagersInitializer().initializeManagers(

            managers,
            managersInitializerCallback,
            persistence = persistence,
            context = applicationContext
        )

        latch.await()

        Assert.assertEquals(0, failed.get())
        Assert.assertEquals(managers.size, registered.get())
    }
}