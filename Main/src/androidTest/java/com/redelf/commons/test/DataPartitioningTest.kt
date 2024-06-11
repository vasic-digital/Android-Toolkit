package com.redelf.commons.test

import com.redelf.commons.logging.Timber
import com.redelf.commons.management.DataManagement
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.persistance.base.Persistence
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DataPartitioningTest : BaseTest() {

    @Before
    fun prepare() {

        Timber.initialize()

        Timber.v("Timber initialized: $this")
    }

    @Test
    fun testPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = true)

        Assert.assertTrue(persistence.isEncryptionEnabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testNoPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testNoPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = true)

        Assert.assertTrue(persistence.isEncryptionEnabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }
}