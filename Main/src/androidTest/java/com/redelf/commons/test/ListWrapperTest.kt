package com.redelf.commons.test

import com.redelf.commons.data.wrappers.ListWrapper
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.test.test_data.Purgable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
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
                    Assert.assertTrue(wrapper.getList() == collection)
                    Assert.assertTrue(wrapper.getSize() == collection.size)

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
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

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
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

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
            Assert.assertTrue(wrapper.getList() == collection)
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
            Assert.assertTrue(wrapper.getList() == collection)
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
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            val first = 1
            val last = 9

            Assert.assertEquals(first, wrapper.getFirst()?.takeData())
            Assert.assertEquals(last, wrapper.getLast()?.takeData())

            Assert.assertEquals(first, wrapper.get(0)?.takeData())
            Assert.assertEquals(last, wrapper.get(wrapper.getSize() - 1)?.takeData())

            val position = 2
            val item = collection[position]

            Assert.assertEquals(wrapper.indexOf(item), position)
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
            Assert.assertTrue(wrapper.getList() == collection)
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
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            wrapper.clear("test")

            yieldWhile(timeoutInMilliseconds = 3000) {

                wrapper.isBusy()
            }

            Assert.assertTrue(wrapper.isEmpty())
            Assert.assertTrue(wrapper.getList().isEmpty())
            Assert.assertTrue(collection.isEmpty())
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
            Assert.assertTrue(wrapper.getList() == collection)
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

//            challengeData.forEachIndexed { index, challenge ->
//
//                Assert.assertEquals(challenge.takeData(), wrapper.get(index)?.takeData())
//            }

//            Assert.assertEquals(challengeData.size, wrapper.getSize())
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
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

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
            Assert.assertTrue(wrapper.getList() == collection)
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
            val wrapper = createWrapper(collection)

            val initSize = collection.size

            Assert.assertTrue(collection.isNotEmpty())
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            wrapper.purge("test")

            yieldWhile(timeoutInMilliseconds = 3000) {

                wrapper.isBusy()
            }

            Assert.assertEquals(initSize - 2, wrapper.getSize())
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
            Assert.assertTrue(wrapper.getList() == collection)
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

    // FIXME: Fix the test
    private fun createWrapper(collection: MutableList<Purgable<Int>>, onUI: Boolean = true) =
        ListWrapper("test", "test.ui=$onUI", onUI, collection)
}