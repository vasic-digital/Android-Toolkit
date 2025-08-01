package com.redelf.commons.test

import com.redelf.commons.data.wrapper.VersionableWrapper
import com.redelf.commons.data.wrapper.list.DefaultListWrapper
import com.redelf.commons.data.wrapper.list.ListWrapperManager
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataPushResult
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.ObtainParametrized
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.test.test_data.Purgable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ListWrapperTest : BaseTest() {

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    /*

        The test asserts the following methods:

        - add
        - get
        - remove index
        - remove item
        - update
        - indexOf
        - removeAll
        - clear
        - replaceAllAndFilter
        - addAllAndFilter
        - addAll
        - purge
        - getSize
    */

    @Test
    fun testAdd() {

        /*
        * TODO: Use the dataAccessManager in other tests
        */
        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                /*
                * We need this callback so the data we assert is in sync until that time ...
                *
                * TODO: Make sure that each test that is using data manager uses callback sync. as well
                *  otherwise, assert may fail because data may be still in aligning!
                */
                listOf(true).forEachIndexed { index, withCallback ->

                    listOf(false, true).forEachIndexed { index, withOnChange ->

                        val pushed = AtomicInteger()

                        val onDataPushed = object : OnObtain<DataPushResult?> {

                            override fun onCompleted(data: DataPushResult?) {

                                if (data?.success == true) {

                                    pushed.incrementAndGet()

                                } else {

                                    Assert.fail("Push has failed")
                                }
                            }


                            override fun onFailure(error: Throwable) {

                                Assert.fail(error.message ?: error::class.simpleName)
                            }
                        }

                        val collection = createCollection()
                        val defaultSize = collection.size
                        val challengeData = createChallengeCollection()
                        val wrapper = createWrapper(collection, onDataPushed = onDataPushed)

                        Assert.assertTrue(collection.isNotEmpty())
                        Assert.assertTrue(challengeData.isNotEmpty())
                        Assert.assertTrue(collection.size == challengeData.size)

                        challengeData.forEachIndexed { challengeIndex, challenge ->

                            val changeDetected = AtomicBoolean()
                            val callbackExecuted = AtomicBoolean()

                            val callback = if (withCallback) {

                                {

                                    callbackExecuted.set(true)
                                }

                            } else {

                                null
                            }

                            val onChange = if (withOnChange) {

                                object : OnChangeCompleted {

                                    override fun onChange(action: String, changed: Boolean) {

                                        changeDetected.set(changed)
                                    }
                                }

                            } else {

                                null
                            }

                            val manager = wrapper.getManager()

                            if (dataAccessManager) {

                                val vWrapper = getVersionableWrapper(wrapper)

                                val d = vWrapper?.takeData()

                                Assert.assertNotNull(d)

                                val added = d?.add(challenge)

                                Assert.assertTrue(added == true)

                                val pushed = manager?.pushData("test", vWrapper, true) == true

                                Assert.assertTrue(pushed)

                            } else {

                                wrapper.add(

                                    from = "testAdd.$challengeIndex",
                                    value = challenge,
                                    onChange = onChange,
                                    callback = callback
                                )
                            }

                            yieldWhile(timeoutInMilliseconds = 3000) {

                                wrapper.isBusy() || manager?.isBusy() == true
                            }

                            if (withOnChange && !dataAccessManager) {

                                yieldWhile(timeoutInMilliseconds = 1000) {

                                    !changeDetected.get()
                                }
                            }

                            if (withCallback && !dataAccessManager) {

                                yieldWhile(timeoutInMilliseconds = 1000) {

                                    !callbackExecuted.get()
                                }
                            }

                            Assert.assertEquals(

                                withOnChange && !dataAccessManager,
                                changeDetected.get()
                            )

                            Assert.assertEquals(

                                withCallback && !dataAccessManager,
                                callbackExecuted.get()
                            )
                        }

                        if (dataAccessManager) {

                            val vWrapper = getVersionableWrapper(wrapper)

                            val d = vWrapper?.takeData()

                            Assert.assertNotNull(d)

                            Assert.assertEquals(defaultSize * 2, d?.size ?: 0)

                            Assert.assertEquals(defaultSize, pushed.get())
                        }

                        val size = wrapper.getSize()

                        Console.log(

                            "$dataAccessManager, $onUI, $withCallback, $withOnChange"
                        )

                        Assert.assertEquals((defaultSize * 2) + 1, size + 1)
                    }
                }
            }
        }
    }

    // TODO: Add to all other tests verification for callback and on change use

    @Test
    fun testGet() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())

                Assert.assertEquals(1, wrapper.get(0)?.takeData())
                Assert.assertEquals(3, wrapper.get(1)?.takeData())
                Assert.assertEquals(5, wrapper.get(2)?.takeData())
            }
        }
    }

    @Test
    fun testRemoveIndex() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())

                wrapper.remove("test", index = 0)

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertEquals(3, wrapper.get(0)?.takeData())
            }
        }
    }

    @Test
    fun testRemoveItem() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val wrapper = createWrapper(collection)
                val what = collection[1]
                val initSize = collection.size

                Assert.assertTrue(true)

                Assert.assertTrue(wrapper.getSize() == collection.size)

                wrapper.remove("test", what = what)

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertEquals(initSize - 1, wrapper.getSize())
            }
        }
    }

    @Test
    fun testUpdate() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val challengeData = createChallengeCollection()
                val wrapper = createWrapper(collection)

                val position = 1
                val original = collection[position]
                val challenge = challengeData[position]

                Assert.assertTrue(collection.size == challengeData.size)

                Assert.assertTrue(wrapper.getSize() == collection.size)

                wrapper.update("test", challenge, 1)

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertEquals(challenge.takeData(), wrapper.get(position)?.takeData())
                Assert.assertFalse(wrapper.contains(original))
            }
        }
    }

    @Test
    fun testIndex() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())

                Assert.assertNotNull(wrapper.getFirst()?.takeData())
                Assert.assertNotNull(wrapper.getLast()?.takeData())
                Assert.assertNotEquals(
                    wrapper.getFirst()?.takeData(),
                    wrapper.getLast()?.takeData()
                )

                val index = 2
                val item = wrapper.get(index)

                Assert.assertNotNull(item)

                item?.let {

                    Assert.assertEquals(index, wrapper.indexOf(it))
                }
            }
        }
    }

    @Test
    fun testRemoveAll() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()

                val duplicates = mutableListOf<Purgable<Int>>()
                duplicates.addAll(collection)

                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())

                Assert.assertTrue(wrapper.getSize() == collection.size)

                wrapper.removeAll("test", listOf(collection.first(), collection.last()))

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertFalse(wrapper.contains(duplicates.first()))
                Assert.assertFalse(wrapper.contains(duplicates.last()))
                Assert.assertEquals(duplicates.size - 2, wrapper.getSize())
            }
        }
    }

    @Test
    fun testClear() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())

                Assert.assertTrue(wrapper.getSize() == collection.size)

                wrapper.clear("test")

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertTrue(wrapper.isEmpty())
                Assert.assertTrue(wrapper.getList().isEmpty())
            }
        }
    }

    @Test
    fun testReplaceAndFilter() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val changeDetected = AtomicBoolean()
                val callbackExecuted = AtomicBoolean()
                val wrapper = createWrapper(collection)
                val challengeData = createChallengeCollection()

                Assert.assertTrue(collection.isNotEmpty())
                Assert.assertTrue(challengeData.isNotEmpty())

                Assert.assertTrue(wrapper.getSize() == collection.size)
                Assert.assertTrue(collection.size == challengeData.size)

                val callback = { modified: Boolean, count: Int ->

                    callbackExecuted.set(true)
                }

                val onChange = object : OnChangeCompleted {

                    override fun onChange(action: String, changed: Boolean) {

                        changeDetected.set(changed)
                    }
                }

                wrapper.replaceAllAndFilter(

                    what = challengeData,
                    from = "test",
                    onChange = onChange,
                    callback = callback
                )

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                yieldWhile(timeoutInMilliseconds = 1000) {

                    !changeDetected.get()
                }

                yieldWhile(timeoutInMilliseconds = 1000) {

                    !callbackExecuted.get()
                }

                challengeData.forEachIndexed { index, challenge ->

                    Assert.assertEquals(challenge.takeData(), wrapper.get(index)?.takeData())
                }

                Assert.assertEquals(challengeData.size, wrapper.getSize())
            }
        }

        // TODO: With and without remove deleted
        // TODO: With and without filter - filter to reverse the collection
    }

    @Test
    fun testAddAllAndFilter() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val challengeData = createChallengeCollection()

                val first = collection.first()
                val last = collection.last()

                challengeData.add(first)
                challengeData.add(last)

                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())
                Assert.assertTrue(challengeData.isNotEmpty())
                Assert.assertTrue(challengeData.size == collection.size + 2)

                wrapper.addAllAndFilter(challengeData, "test")

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertEquals(challengeData.size, wrapper.getSize())

                challengeData.forEach { challenge ->

                    Assert.assertTrue(wrapper.contains(challenge))
                }

                Assert.assertTrue(wrapper.contains(first))
                Assert.assertTrue(wrapper.contains(last))
            }
        }

        // TODO: With and without remove deleted
        // TODO: With and without filter - filter to reverse the collection
    }

    @Test
    fun testAddAll() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val challengeData = createChallengeCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())
                Assert.assertTrue(challengeData.isNotEmpty())
                Assert.assertTrue(collection.size == challengeData.size)

                Assert.assertTrue(wrapper.getSize() == collection.size)

                wrapper.addAll(challengeData, "testAdd.all")

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                val size = wrapper.getSize()
                val defaultSize = createCollection().size

                Assert.assertTrue(size > defaultSize)
                Assert.assertTrue(size == defaultSize + challengeData.size)

                challengeData.forEach { challenge ->

                    Assert.assertTrue(wrapper.contains(challenge))
                    Assert.assertTrue(wrapper.getList().contains(challenge))
                }
            }
        }
    }

    @Test
    fun testPurge() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection(hasDeletedItems = true)
                val expected = collection.size - 2
                val wrapper = createWrapper(collection, expectedSize = expected)

                Assert.assertTrue(collection.isNotEmpty())

                wrapper.purge("test")

                yieldWhile(timeoutInMilliseconds = 3000) {

                    wrapper.isBusy()
                }

                Assert.assertEquals(expected, wrapper.getSize())
            }
        }
    }

    @Test
    fun testGetSize() {

        listOf(false, true).forEach { dataAccessManager ->

            listOf(true, false).forEachIndexed { index, onUI ->

                val collection = createCollection()
                val challengeData = createChallengeCollection()
                val wrapper = createWrapper(collection)

                Assert.assertTrue(collection.isNotEmpty())
                Assert.assertTrue(challengeData.isNotEmpty())
                Assert.assertTrue(collection.size == challengeData.size)

                Assert.assertTrue(wrapper.getSize() == collection.size)
            }
        }
    }

    private fun createCollection(hasDeletedItems: Boolean = false) =
        mutableListOf(
            Purgable(1),
            Purgable(3, hasDeletedItems),
            Purgable(5, hasDeletedItems),
            Purgable(7),
            Purgable(9)
        )

    private fun createChallengeCollection() =
        mutableListOf(Purgable(2), Purgable(4), Purgable(6), Purgable(8), Purgable(10))

    private fun createWrapper(

        collection: MutableList<Purgable<Int>>,
        onUI: Boolean = true,
        expectedSize: Int = collection.size,
        onDataPushed: OnObtain<DataPushResult?>? = null,

        ): DefaultListWrapper<Purgable<Int>, Int> {

        val wrapper = DefaultListWrapper(

            onUi = onUI,
            lazySaving = false,
            persistData = false,
            onDataPushed = onDataPushed,
            identifier = "test.${System.currentTimeMillis()}",

            identifierObtainer = object : ObtainParametrized<Int, Purgable<Int>> {

                override fun obtain(param: Purgable<Int>): Int {

                    return param.getId() ?: 0
                }
            },

            creator = object : Obtain<VersionableWrapper<CopyOnWriteArraySet<Purgable<Int>>>> {

                override fun obtain(): VersionableWrapper<CopyOnWriteArraySet<Purgable<Int>>> {

                    val collection = CopyOnWriteArraySet(collection)

                    return VersionableWrapper(collection)
                }
            }
        )

        yieldWhile {

            wrapper.isNotInitialized()
        }

        Assert.assertEquals(expectedSize, wrapper.getSize())

        return wrapper
    }

    private fun getManager(wrapper: DefaultListWrapper<Purgable<Int>, Int>): ListWrapperManager<Purgable<Int>>? {

        val manager = wrapper.getManager()

        Assert.assertNotNull(manager)

        return manager
    }

    private fun getVersionableWrapper(wrapper: DefaultListWrapper<Purgable<Int>, Int>): VersionableWrapper<CopyOnWriteArraySet<Purgable<Int>>>? {

        val manager = getManager(wrapper)

        try {

            val versionableWrapper = manager?.obtain()

            Assert.assertNotNull(versionableWrapper)

            return versionableWrapper

        } catch (e: Throwable) {

            Assert.fail(e.message)
        }

        return null
    }
}