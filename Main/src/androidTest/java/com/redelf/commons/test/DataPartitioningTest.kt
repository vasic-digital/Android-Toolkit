package com.redelf.commons.test

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.logging.Timber
import com.redelf.commons.test.data.LongListWrapper
import com.redelf.commons.test.data.LongWrapper
import com.redelf.commons.test.data.ObjectListWrapper
import com.redelf.commons.test.data.ObjectMapWrapper
import com.redelf.commons.test.data.SampleData2
import com.redelf.commons.test.data.SampleData3
import com.redelf.commons.test.data.SampleData
import com.redelf.commons.test.data.SampleDataOnlyP2
import com.redelf.commons.test.data.StringListWrapper
import com.redelf.commons.test.data.StringWrapper
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

        Timber.initialize(failOnError = true)

        Timber.v("Timber initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
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
    fun testLong() {

        val long = System.currentTimeMillis()
        val wrapper = LongWrapper(long)

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.Long.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<LongWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedItem = wrapper.takeData()
        val comparableItem = comparable?.takeData()

        Assert.assertNotNull(wrappedItem)

        Assert.assertEquals(wrappedItem, comparableItem)
    }

    @Test
    fun testString() {

        val str = sampleUUID.toString()
        val wrapper = StringWrapper(str)

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.String.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<StringWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedItem = wrapper.takeData()
        val comparableItem = comparable?.takeData()

        Assert.assertNotNull(wrappedItem)

        Assert.assertEquals(wrappedItem, comparableItem)
    }

    @Test
    fun testLongList() {

        val list = CopyOnWriteArrayList<Long>()
        val wrapper = LongListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(x.toLong())
        }

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.List.Long.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<LongListWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedList = wrapper.takeData()
        val comparableList = comparable?.takeData()

        Assert.assertEquals(wrappedList, comparableList)
    }

    @Test
    fun testComplexList() {

        val list = CopyOnWriteArrayList<SampleData3>()
        val wrapper = ObjectListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedDataSecondLevel(x))
        }

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.List.Complex.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<ObjectListWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedList = wrapper.takeData()
        val comparableList = comparable?.takeData()

        Assert.assertEquals(wrappedList, comparableList)
    }

    @Test
    fun testComplexMap() {

        val map = ConcurrentHashMap<UUID, SampleData3>()
        val wrapper = ObjectMapWrapper(map)

        for (x in 0..samplesCount) {

            val uuid = UUID(x.toLong(), x.toLong())

            map[uuid] = instantiateTestNestedDataSecondLevel(x)
        }

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.Map.Complex.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<ObjectMapWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedList = wrapper.takeData()
        val comparableList = comparable?.takeData()

        Assert.assertEquals(wrappedList, comparableList)
    }

    @Test
    fun testStringsList() {

        val list = CopyOnWriteArrayList<String>()
        val wrapper = StringListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(x.toString())
        }

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val key = "Test.List.String.No_Enc"
        val saved = persistence.push(key, wrapper)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<StringListWrapper?>(key)

        Assert.assertNotNull(comparable)

        val wrappedList = wrapper.takeData()
        val comparableList = comparable?.takeData()

        Assert.assertEquals(wrappedList, comparableList)
    }

    @Test
    fun testPartition2() {

        val persistence = instantiatePersistenceAndInitialize(doEncrypt = false)

        Assert.assertTrue(persistence.isEncryptionDisabled())

        val data = instantiateTestDataP2()

        Assert.assertTrue(data.isPartitioningEnabled())

        val key = "Test.Part.P2.No_Enc"

        val saved = persistence.push(key, data)

        Assert.assertTrue(saved)

        val comparable = persistence.pull<SampleDataOnlyP2?>(key)

        Assert.assertNotNull(comparable)

        Assert.assertEquals(data, comparable)
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

        val comparable = persistence.pull<SampleData?>(key)

        Assert.assertNotNull(comparable)

        Assert.assertEquals(data.partition1, comparable?.partition1)
        Assert.assertEquals(data.partition2, comparable?.partition2)
        Assert.assertEquals(data.partition3, comparable?.partition3)
        Assert.assertEquals(data.partition4, comparable?.partition4)
        Assert.assertEquals(data.partition5, comparable?.partition5)
        Assert.assertEquals(data.partition6, comparable?.partition6)

        Assert.assertEquals(data, comparable)
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

    private fun instantiateTestData(partitioning: Boolean): SampleData {

        return SampleData(

            partitioningOn = partitioning,
            partition1 = createPartition1(),
            partition2 = createPartition2(),
            partition3 = createPartition3(),
            partition4 = createPartition4(),
            partition5 = createPartition5(),
            partition6 = createPartition6(),
        )
    }

    private fun instantiateTestDataP2(): SampleDataOnlyP2 {

        return SampleDataOnlyP2(

            partitioningOn = true,
            partition2 = createPartition2()
        )
    }

    private fun assertTestData(partitioning: Boolean, source: SampleData) {

        val comparable = instantiateTestData(partitioning = partitioning)

        Assert.assertEquals(comparable, source)
    }

    private fun createPartition1(): CopyOnWriteArrayList<SampleData2> {

        val list = CopyOnWriteArrayList<SampleData2>()

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedData(x))
        }

        return list
    }

    private fun createPartition2(): ConcurrentHashMap<UUID, SampleData2> {

        val map = ConcurrentHashMap<UUID, SampleData2>()

        for (x in 0..samplesCount) {

            val uuid = UUID(x.toLong(), x.toLong())

            map[uuid] = instantiateTestNestedData(x)
        }

        return map
    }

    private fun createPartition3(): ConcurrentHashMap<String, List<SampleData3>> {

        val map = ConcurrentHashMap<String, List<SampleData3>>()

        for (x in 0..samplesCount) {

            val list = mutableListOf<SampleData3>()

            for (y in 0..samplesCount) {

                list.add(instantiateTestNestedDataSecondLevel(y))
            }

            map[sampleUUID.toString()] = list
        }

        return map
    }

    private fun createPartition4(): SampleData3 {

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

    private fun instantiateTestNestedData(sample: Int): SampleData2 {

        val list = CopyOnWriteArrayList<SampleData3>()

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedDataSecondLevel(x))
        }

        return SampleData2(

            id = sampleUUID,
            isEnabled = sample % 2 == 0,
            order = sample.toLong(),
            title = sample.toString(),
            nested = list
        )
    }

    private fun assertNestedData(source: SampleData2, sample: Int) {

        val comparable = instantiateTestNestedData(sample)

        Assert.assertEquals(comparable, source)
    }

    private fun instantiateTestNestedDataSecondLevel(sample: Int): SampleData3 {

        val list = mutableListOf<String>()

        for (x in 0..samplesCount) {

            list.add(x.toString())
        }

        return SampleData3(

            id = sampleUUID,
            title = sample.toString(),
            order = sample.toLong(),
            points = list
        )
    }

    private fun assertNestedDataSecondLevel(source: SampleData3, sample: Int) {

        val comparable = instantiateTestNestedDataSecondLevel(sample)

        Assert.assertEquals(comparable, source)
    }
}