package com.redelf.commons.test

import com.redelf.commons.logging.Timber
import com.redelf.commons.model.Wrapper
import com.redelf.commons.partition.Partitional
import com.redelf.commons.test.data.NestedData
import com.redelf.commons.test.data.NestedDataSecondLevel
import com.redelf.commons.test.data.PartitioningTestData
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

import com.google.gson.reflect.TypeToken

class DataPartitioningTest : BaseTest() {

    private val samplesCount = 5
    private val sampleUUID = UUID.randomUUID()

    private class ListWrapper(list: CopyOnWriteArrayList<Long>) :

        Wrapper<CopyOnWriteArrayList<Long>>(list),
        Partitional<ListWrapper> {

        override fun isPartitioningEnabled() = true

        override fun getPartitionCount() = 1

        override fun getPartitionData(number: Int): Any {

            if (number > 0) {

                Assert.fail("Unexpected partition number: $number")
            }

            return takeData()
        }

        @Suppress("UNCHECKED_CAST")
        override fun setPartitionData(number: Int, data: Any?): Boolean {

            if (number > 0) {

                Assert.fail("Unexpected partition number: $number")
            }

            try {

                this.data = data as CopyOnWriteArrayList<Long>

            } catch (e: Exception) {

                Timber.e(e)

                return false
            }

            return true
        }

        override fun getPartitionType(number: Int): Type? {

            if (number > 0) {

                Assert.fail("Unexpected partition number: $number")
            }

            return object : TypeToken<CopyOnWriteArrayList<Long>>() {}.type
        }

        override fun getClazz(): Class<ListWrapper> {

            return ListWrapper::class.java
        }
    }

    @Before
    fun prepare() {

        Timber.initialize()

        Timber.v("Timber initialized: $this")
    }

    @Test
    fun testAssert() {

        for (x in 0..samplesCount) {

            val sample = instantiateTestNestedDataSecondLevel(x)
            assertNestedDataSecondLevel(sample, x)
        }

        for (x in 0..samplesCount) {

            val sample = instantiateTestNestedData(x)
            assertNestedData(sample, x)
        }

        for (x in 0..samplesCount) {

            val partitioning = x % 2 == 0
            val sample = instantiateTestData(partitioning = partitioning)
            assertTestData(partitioning, sample)
        }
    }


    @Test
    fun testList() {

        val list = CopyOnWriteArrayList<Long>()
        val wrapper = ListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(x.toLong())
        }

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.List.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<ListWrapper?>(key)

        Assert.assertNotNull(comparable)

        //        Assert.assertEquals(wrapper, comparable)
    }

    @Test
    fun testPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val data = instantiateTestData(partitioning = true)

        Assert.assertTrue(data.isPartitioningEnabled())

        val key = "Test.Part.No_Enc"

        val saved = persistence.push(key, data)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<PartitioningTestData?>(key)

        Assert.assertNotNull(comparable)

        // FIXME:
        //
        //        Assert.assertEquals(data, comparable)
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

    private fun assertTestData(partitioning: Boolean, source: PartitioningTestData) {

        val comparable = instantiateTestData(partitioning = partitioning)

        Assert.assertEquals(comparable, source)
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

    private fun assertNestedData(source: NestedData, sample: Int) {

        val comparable = instantiateTestNestedData(sample)

        Assert.assertEquals(comparable, source)
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

    private fun assertNestedDataSecondLevel(source: NestedDataSecondLevel, sample: Int) {

        val comparable = instantiateTestNestedDataSecondLevel(sample)

        Assert.assertEquals(comparable, source)
    }
}