package com.redelf.commons.test

import com.redelf.commons.logging.Timber
import com.redelf.commons.management.DataManagement
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.persistance.base.Persistence
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

        val persistence = instantiatePersistence(doEncrypt = false)

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testPartitioningWithEncryption() {

        val persistence = instantiatePersistence(doEncrypt = true)

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testNoPartitioning() {

        // TODO: Implement proper test
        assert(5 == 5)
    }
}