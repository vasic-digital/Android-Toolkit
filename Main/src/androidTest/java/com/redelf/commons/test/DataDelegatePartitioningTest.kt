package com.redelf.commons.test

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.logging.Console
import com.redelf.commons.test.test_data.SampleData
import com.redelf.commons.test.test_data.SampleData2
import com.redelf.commons.test.test_data.SampleData3
import com.redelf.commons.test.test_data.SampleDataOnlyP2
import com.redelf.commons.test.test_data.SampleDataOnlyP3
import com.redelf.commons.test.test_data.SampleDataOnlyP4
import com.redelf.commons.test.test_data.SampleDataOnlyP5
import com.redelf.commons.test.test_data.wrapper.BoolListWrapper
import com.redelf.commons.test.test_data.wrapper.BoolWrapper
import com.redelf.commons.test.test_data.wrapper.LongListWrapper
import com.redelf.commons.test.test_data.wrapper.LongWrapper
import com.redelf.commons.test.test_data.wrapper.ObjectListWrapper
import com.redelf.commons.test.test_data.wrapper.ObjectMapWrapper
import com.redelf.commons.test.test_data.wrapper.StringListWrapper
import com.redelf.commons.test.test_data.wrapper.StringToLongMapWrapper
import com.redelf.commons.test.test_data.wrapper.StringWrapper
import com.redelf.commons.test.test_data.wrapper.UUIDtoStringMapWrapper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DataDelegatePartitioningTest : BaseTest() {

    private val samplesCount = 5
    private val sampleUUID = UUID.randomUUID()

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

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

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

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
    }

    @Test
    fun testBoolean() {

        val bool: Boolean = System.currentTimeMillis() % 2L == 0L
        val wrapper = BoolWrapper(bool)

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.Bool.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<BoolWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedItem = wrapper.takeData()
            val comparableItem = comparable?.takeData()

            Assert.assertNotNull(wrappedItem)

            Assert.assertEquals(wrappedItem, comparableItem)
        }
    }

    @Test
    fun testString() {

        val str = sampleUUID.toString()
        val wrapper = StringWrapper(str)

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

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
    }

    @Test
    fun testLongList() {

        val list = CopyOnWriteArrayList<Double>()
        val wrapper = LongListWrapper(list)

        (0..samplesCount).forEach { x ->

            list.add(System.currentTimeMillis().toDouble())
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.List.Long.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<LongListWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedList = wrapper.takeData()
            val comparableList = comparable?.takeData()

            Assert.assertEquals(wrappedList, comparableList)
        }
    }

    @Test
    fun testBoolList() {

        val list = CopyOnWriteArrayList<Boolean>()
        val wrapper = BoolListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(x % 2 == 0)
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.List.Bool.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<BoolListWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedList = wrapper.takeData()
            val comparableList = comparable?.takeData()

            Assert.assertEquals(wrappedList, comparableList)
        }
    }

    @Test
    fun testStringsList() {

        val list = CopyOnWriteArrayList<String>()
        val wrapper = StringListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(UUID(x.toLong(), x.toLong()).toString())
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.List.String.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<StringListWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedList = wrapper.takeData()
            val comparableList = comparable?.takeData()

            Assert.assertEquals(wrappedList, comparableList)
        }
    }

    @Test
    fun testComplexList() {

        val list = CopyOnWriteArrayList<SampleData3>()
        val wrapper = ObjectListWrapper(list)

        for (x in 0..samplesCount) {

            list.add(instantiateTestNestedDataSecondLevel(x))
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.List.Complex.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<ObjectListWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedList = wrapper.takeData()
            val comparableList = comparable?.takeData()

            Assert.assertEquals(wrappedList, comparableList)
        }
    }

    @Test
    fun testUUIDtoStringMap() {

        val map = ConcurrentHashMap<UUID, String>()
        val wrapper = UUIDtoStringMapWrapper(map)

        for (x in 0..samplesCount) {

            val uuid = UUID(x.toLong(), x.toLong())

            map[uuid] = x.toString()
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.Map.UUIDtoString.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<UUIDtoStringMapWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedMap = wrapper.takeData()
            val comparableMap = comparable?.takeData()

            assertMaps(wrappedMap?.toMap(), comparableMap?.toMap())
        }
    }

    @Test
    fun testStringToLongMap() {

        val map = ConcurrentHashMap<String, Long>()
        val wrapper = StringToLongMapWrapper(map)

        for (x in 0..samplesCount) {

            val uuid = UUID(x.toLong(), x.toLong())

            map[uuid.toString()] = System.currentTimeMillis()
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.Map.StringToLong.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<StringToLongMapWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedMap = wrapper.takeData()
            val comparableMap = comparable?.takeData()

            assertMaps(wrappedMap?.toMap(), comparableMap?.toMap())
        }
    }

    @Test
    fun testComplexMap() {

        val map = ConcurrentHashMap<UUID, SampleData3>()
        val wrapper = ObjectMapWrapper(map)

        for (x in 0..samplesCount) {

            val uuid = UUID(x.toLong(), x.toLong())

            map[uuid] = instantiateTestNestedDataSecondLevel(x)
        }

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val key = "Test.Map.Complex.No_Enc"
            val saved = persistence.push(key, wrapper)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<ObjectMapWrapper?>(key)

            Assert.assertNotNull(comparable)

            val wrappedMap = wrapper.takeData()
            val comparableMap = comparable?.takeData()

            assertMaps(wrappedMap?.toMap(), comparableMap?.toMap())
        }
    }

    @Test
    fun testPartition2() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestDataP2()

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.P2.No_Enc"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleDataOnlyP2?>(key)

            Assert.assertNotNull(comparable)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testPartition3() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestDataP3()

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.P3.No_Enc"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleDataOnlyP3?>(key)

            Assert.assertNotNull(comparable)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testPartition4() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestDataP4()

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.P4.No_Enc"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleDataOnlyP4?>(key)

            Assert.assertNotNull(comparable)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testPartition5() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestDataP5()

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.P5.No_Enc"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleDataOnlyP5?>(key)

            Assert.assertNotNull(comparable)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

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
    }

    @Test
    fun testPartitioningWithNoEncryptionSync() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestData(partitioning = true, async = false)

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.No_Enc.Sync"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleData?>(key)

            Assert.assertNotNull(comparable)

            comparable?.setPartitioningParallelized(false)

            Assert.assertEquals(data.partition1, comparable?.partition1)
            Assert.assertEquals(data.partition2, comparable?.partition2)
            Assert.assertEquals(data.partition3, comparable?.partition3)
            Assert.assertEquals(data.partition4, comparable?.partition4)
            Assert.assertEquals(data.partition5, comparable?.partition5)
            Assert.assertEquals(data.partition6, comparable?.partition6)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestData(partitioning = true)

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.Enc"

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
    }

    @Test
    fun testPartitioningWithEncryptionSync() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertTrue(persistence?.isEncryptionEnabled() == true)

        Assert.assertNotNull(persistence)

        persistence?.let {

            val data = instantiateTestData(partitioning = true, async = false)

            Assert.assertTrue(data.isPartitioningEnabled())

            val key = "Test.Part.Enc.Sync"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleData?>(key)

            Assert.assertNotNull(comparable)

            comparable?.setPartitioningParallelized(false)

            Assert.assertEquals(data.partition1, comparable?.partition1)
            Assert.assertEquals(data.partition2, comparable?.partition2)
            Assert.assertEquals(data.partition3, comparable?.partition3)
            Assert.assertEquals(data.partition4, comparable?.partition4)
            Assert.assertEquals(data.partition5, comparable?.partition5)
            Assert.assertEquals(data.partition6, comparable?.partition6)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testNoPartitioningWithNoEncryption() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestData(partitioning = false)

            Assert.assertTrue(data.isPartitioningDisabled())

            val key = "Test.No_Part.No_Enc"

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
    }

    @Test
    fun testNoPartitioningWithNoEncryptionSync() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestData(partitioning = false, async = false)

            Assert.assertTrue(data.isPartitioningDisabled())

            val key = "Test.No_Part.No_Enc.Sync"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleData?>(key)

            Assert.assertNotNull(comparable)

            comparable?.setPartitioningParallelized(false)

            Assert.assertEquals(data.partition1, comparable?.partition1)
            Assert.assertEquals(data.partition2, comparable?.partition2)
            Assert.assertEquals(data.partition3, comparable?.partition3)
            Assert.assertEquals(data.partition4, comparable?.partition4)
            Assert.assertEquals(data.partition5, comparable?.partition5)
            Assert.assertEquals(data.partition6, comparable?.partition6)

            Assert.assertEquals(data, comparable)
        }
    }

    @Test
    fun testNoPartitioningWithEncryption() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertNotNull(persistence)

        persistence?.let {

            Assert.assertTrue(persistence.isEncryptionEnabled())

            val data = instantiateTestData(partitioning = false)

            Assert.assertTrue(data.isPartitioningDisabled())

            val key = "Test.No_Part.Enc"

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
    }

    @Test
    fun testNoPartitioningWithEncryptionSync() {

        val persistence = instantiatePersistenceAndInitialize()

        Assert.assertTrue(persistence?.isEncryptionEnabled() == true)

        Assert.assertNotNull(persistence)

        persistence?.let {

            val data = instantiateTestData(partitioning = false, async = false)

            Assert.assertTrue(data.isPartitioningDisabled())

            val key = "Test.No_Part.Enc.Sync"

            val saved = persistence.push(key, data)

            Assert.assertTrue(saved)

            val comparable = persistence.pull<SampleData?>(key)

            Assert.assertNotNull(comparable)

            comparable?.setPartitioningParallelized(false)

            Assert.assertEquals(data.partition1, comparable?.partition1)
            Assert.assertEquals(data.partition2, comparable?.partition2)
            Assert.assertEquals(data.partition3, comparable?.partition3)
            Assert.assertEquals(data.partition4, comparable?.partition4)
            Assert.assertEquals(data.partition5, comparable?.partition5)
            Assert.assertEquals(data.partition6, comparable?.partition6)

            Assert.assertEquals(data, comparable)
        }
    }

    private fun instantiateTestData(partitioning: Boolean, async: Boolean = true): SampleData {

        return SampleData(

            partitioningOn = partitioning,
            partitioningParallelized = async,

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

    private fun instantiateTestDataP3(): SampleDataOnlyP3 {

        return SampleDataOnlyP3(

            partitioningOn = true,
            partition3 = createPartition3()
        )
    }

    private fun instantiateTestDataP4(): SampleDataOnlyP4 {

        return SampleDataOnlyP4(

            partitioningOn = true,
            partition4 = createPartition4()
        )
    }

    private fun instantiateTestDataP5(): SampleDataOnlyP5 {

        return SampleDataOnlyP5(

            partitioningOn = true,
            partition5 = createPartition5()
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

        (0..samplesCount).forEach { x ->

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

    private fun createPartition6(): CopyOnWriteArrayList<Double> {

        val list = CopyOnWriteArrayList<Double>()

        for (x in 0..samplesCount) {

            list.add(x.toDouble())
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

    private fun assertMaps(map1: Map<*, *>?, map2: Map<*, *>?) {

        Assert.assertNotNull(map1)
        Assert.assertNotNull(map2)

        map1?.let { m1 ->
            map2?.let { m2 ->

                Assert.assertEquals(m1.size, m2.size)

                val k1 = m1.keys.toList().sortedBy { it.toString() }
                val k2 = m2.keys.toList().sortedBy { it.toString() }

                Assert.assertEquals(k1, k2)

                m1.forEach { (key, value) ->

                    Assert.assertNotNull(key)
                    Assert.assertNotNull(value)

                    val comparable = m2[key]

                    Assert.assertNotNull(comparable)
                    Assert.assertEquals(value, comparable)
                }
            }
        }
    }
}