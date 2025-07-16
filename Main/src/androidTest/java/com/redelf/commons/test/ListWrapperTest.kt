package com.redelf.commons.test

import com.redelf.commons.data.wrapper.VersionableWrapper
import com.redelf.commons.data.wrapper.list.DefaultListWrapper
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.test.test_data.Purgable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

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

        listOf(true, false).forEachIndexed { index, onUI ->

            listOf(false, true).forEachIndexed { index, withCallback ->

                listOf(false, true).forEachIndexed { index, withOnChange ->

                    val collection = createCollection()
                    val wrapper = createWrapper(collection)
                    val challengeData = createChallengeCollection()

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

                        wrapper.add(

                            from = "testAdd.$challengeIndex",
                            value = challenge,
                            onChange = onChange,
                            callback = callback
                        )

                        yieldWhile(timeoutInMilliseconds = 3000) {

                            wrapper.isBusy()
                        }

                        if (withOnChange) {

                            yieldWhile(timeoutInMilliseconds = 1000) {

                                !changeDetected.get()
                            }
                        }

                        if (withCallback) {

                            yieldWhile(timeoutInMilliseconds = 1000) {

                                !callbackExecuted.get()
                            }
                        }

                        val size = wrapper.getSize()
                        val defaultSize = createCollection().size

                        Assert.assertTrue(size > defaultSize)
                        Assert.assertTrue(size == defaultSize + (challengeIndex + 1))
                        Assert.assertTrue(wrapper.contains(challenge))
                        Assert.assertTrue(wrapper.getList().contains(challenge))

                        Assert.assertEquals(withOnChange, changeDetected.get())
                        Assert.assertEquals(withCallback, callbackExecuted.get())
                    }
                }
            }
        }
    }

    // TODO: Add to all other tests verification for callback and on change use

    @Test
    fun testGet() {

        listOf(true, false).forEachIndexed { index, onUI ->

            val collection = createCollection()
            val wrapper = createWrapper(collection)

            Assert.assertTrue(collection.isNotEmpty())

            Assert.assertEquals(1, wrapper.get(0)?.takeData())
            Assert.assertEquals(3, wrapper.get(1)?.takeData())
            Assert.assertEquals(5, wrapper.get(2)?.takeData())
        }
    }

    @Test
    fun testRemoveIndex() {

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

    @Test
    fun testRemoveItem() {

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

    @Test
    fun testUpdate() {

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

    @Test
    fun testIndex() {

        listOf(true, false).forEachIndexed { index, onUI ->

            val collection = createCollection()
            val wrapper = createWrapper(collection)

            Assert.assertTrue(collection.isNotEmpty())

            Assert.assertNotNull(wrapper.getFirst()?.takeData())
            Assert.assertNotNull(wrapper.getLast()?.takeData())
            Assert.assertNotEquals(wrapper.getFirst()?.takeData(), wrapper.getLast()?.takeData())

            val index = 2
            val item = wrapper.get(index)

            Assert.assertNotNull(item)

            item?.let {

                Assert.assertEquals(index, wrapper.indexOf(it))
            }
        }
    }

    @Test
    fun testRemoveAll() {

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

    @Test
    fun testClear() {

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

    @Test
    fun testReplaceAndFilter() {

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

        // TODO: With and without remove deleted
        // TODO: With and without filter - filter to reverse the collection
    }

    @Test
    fun testAddAllAndFilter() {

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

        // TODO: With and without remove deleted
        // TODO: With and without filter - filter to reverse the collection
    }

    @Test
    fun testAddAll() {

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

    @Test
    fun testPurge() {

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

    @Test
    fun testGetSize() {

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
        expectedSize: Int = collection.size

    ): DefaultListWrapper<Purgable<Int>> {

        val wrapper = DefaultListWrapper(

            onUi = onUI,
            lazySaving = false,
            persistData = false,
            identifier = "test.${System.currentTimeMillis()}",

            creator = object : Obtain<VersionableWrapper<CopyOnWriteArrayList<Purgable<Int>>>> {

                override fun obtain(): VersionableWrapper<CopyOnWriteArrayList<Purgable<Int>>> {

                    val collection = CopyOnWriteArrayList(collection)

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
}