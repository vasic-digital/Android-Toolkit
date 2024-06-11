package com.redelf.commons.test

import com.redelf.commons.logging.Timber
import org.junit.Before
import org.junit.Test

class DataPartitioningTest : BaseTest() {

    @Before
    fun prepare() {

        Timber.initialize()

        Timber.v("Timber initialized: $this")
    }

    @Test
    fun testPartitioning() {

        // TODO: Implement proper test
        assert(5 == 5)
    }

    @Test
    fun testNoPartitioning() {

        // TODO: Implement proper test
        assert(5 == 5)
    }
}