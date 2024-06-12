package com.redelf.commons.test

import com.redelf.commons.logging.Timber
import com.redelf.commons.management.DataManagement
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.persistance.base.Persistence
import com.redelf.commons.test.data.NestedData
import com.redelf.commons.test.data.NestedDataSecondLevel
import com.redelf.commons.test.data.PartitioningTestData
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DataPartitioningTest : BaseTest() {

    private val samplesCount = 5
    private val sampleUUID = UUID.randomUUID()

    @Before
    fun prepare() {

        Timber.initialize()

        Timber.v("Timber initialized: $this")
    }

    @Test
    fun testPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val data = instantiateTestData(partitioning = true)

        Assert.assertTrue(data.isPartitioningEnabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = true)

        Assert.assertTrue(persistence.isEncryptionEnabled())

        val data = instantiateTestData(partitioning = true)

        Assert.assertTrue(data.isPartitioningEnabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testNoPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val data = instantiateTestData(partitioning = false)

        Assert.assertTrue(data.isPartitioningDisabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    @Test
    fun testNoPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = true)

        Assert.assertTrue(persistence.isEncryptionEnabled())

        val data = instantiateTestData(partitioning = false)

        Assert.assertTrue(data.isPartitioningDisabled())

        // TODO: Implement proper test

        assert(5 == 5)
    }

    private fun instantiateTestData(partitioning: Boolean): PartitioningTestData {

        return PartitioningTestData(

            partitioningOn = partitioning,
            partition1 = createPartition1(),
            partition2 = createPartition2(),
            partition3 = createPartition3(),
            partition4 = createPartition4(),
            partition5 = createPartition5(),
            partition6 = createPartition6(),
        )
    }

    private fun createPartition1(): CopyOnWriteArrayList<NestedData> {

        val list = CopyOnWriteArrayList<NestedData>()

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedData(x))
        }

        return list
    }

    private fun createPartition2(): ConcurrentHashMap<UUID, NestedData> {

        val map = ConcurrentHashMap<UUID, NestedData>()

        for (x in 0..samplesCount) {

            map[UUID(x.toLong(), x.toLong())] = instantiateTestNestedData(x)
        }

        return map
    }

    private fun createPartition3(): ConcurrentHashMap<String, List<NestedDataSecondLevel>> {

        val map = ConcurrentHashMap<String, List<NestedDataSecondLevel>>()

        for (x in 0..samplesCount) {

            val list = mutableListOf<NestedDataSecondLevel>()

            for (y in 0..samplesCount) {

                list.add(instantiateTestNestedDataSecondLevel(y))
            }

            map[sampleUUID.toString()] = list
        }

        return map
    }

    private fun createPartition4(): NestedDataSecondLevel {

        return instantiateTestNestedDataSecondLevel(0)
    }

    private fun createPartition5(): String = sampleUUID.toString()

    private fun createPartition6(): CopyOnWriteArrayList<Long> {

        val list = CopyOnWriteArrayList<Long>()

        for (x in 0..samplesCount) {

            list.add(x.toLong())
        }

        return list
    }

    private fun instantiateTestNestedData(sample: Int = 0): NestedData {

        val list = CopyOnWriteArrayList<NestedDataSecondLevel>()

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedDataSecondLevel(x))
        }

        return NestedData(

            id = sampleUUID,
            isEnabled = sample % 2 == 0,
            order = sample.toLong(),
            title = sample.toString(),
            nested = list
        )
    }

    private fun instantiateTestNestedDataSecondLevel(sample: Int = 0): NestedDataSecondLevel {

        val list = mutableListOf<String>()

        for (x in 0..samplesCount) {

            list.add(x.toString())
        }

        return NestedDataSecondLevel(

            id = sampleUUID,
            title = sample.toString(),
            order = sample.toLong(),
            points = list
        )
    }
}