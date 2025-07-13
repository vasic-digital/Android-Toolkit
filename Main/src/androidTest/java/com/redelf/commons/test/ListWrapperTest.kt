package com.redelf.commons.test

import com.redelf.commons.data.wrappers.Wrapper
import com.redelf.commons.data.wrappers.ListWrapper
import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.logging.Console
import org.junit.Assert
import org.junit.Before
import org.junit.Test

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

            val collection = createCollection()
            val wrapper = createWrapper(collection)

            val challengeData = createChallengeCollection()

            Assert.assertTrue(collection.isNotEmpty())
            Assert.assertTrue(challengeData.isNotEmpty())
            Assert.assertTrue(collection.size == challengeData.size)
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            challengeData.forEachIndexed { challengeIndex, challenge ->

                wrapper.add("testAdd.$challengeIndex", challenge)

                val size = wrapper.getSize()
                val defaultSize = createCollection().size

                Assert.assertTrue(size > defaultSize)
                Assert.assertTrue(size == defaultSize + (challengeIndex + 1))
                Assert.assertTrue(wrapper.contains(challenge))
                Assert.assertTrue(wrapper.getList().contains(challenge))
            }
        }
    }

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

            Assert.assertEquals( challenge.takeData(), wrapper.get(position)?.takeData())
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

            val duplicates = mutableListOf<Wrapper<Int>>()
            duplicates.addAll(collection)

            val wrapper = createWrapper(collection)

            Assert.assertTrue(collection.isNotEmpty())
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            wrapper.removeAll("test", listOf(collection.first(), collection.last()))

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

            Assert.assertTrue(wrapper.isEmpty())
            Assert.assertTrue(wrapper.getList().isEmpty())
            Assert.assertTrue(collection.isEmpty())
        }
    }

    @Test
    fun testReplaceAndFilter() {

        listOf(true, false).forEachIndexed { index, onUI ->

            val collection = createCollection()
            val challengeData = createChallengeCollection()
            val wrapper = createWrapper(collection)

            Assert.assertTrue(collection.isNotEmpty())
            Assert.assertTrue(challengeData.isNotEmpty())
            Assert.assertTrue(collection.size == challengeData.size)
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

            wrapper.replaceAllAndFilter(challengeData, "test")

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
            val wrapper = createWrapper(collection)

            Assert.assertTrue(collection.isNotEmpty())
            Assert.assertTrue(challengeData.isNotEmpty())
            Assert.assertTrue(collection.size == challengeData.size)
            Assert.assertTrue(wrapper.getList() == collection)
            Assert.assertTrue(wrapper.getSize() == collection.size)

        }


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

    private fun createCollection() =
        mutableListOf(Wrapper(1), Wrapper(3), Wrapper(5), Wrapper(7), Wrapper(9))

    private fun createChallengeCollection() =
        mutableListOf(Wrapper(2), Wrapper(4), Wrapper(6), Wrapper(8), Wrapper(10))

    private fun createWrapper(collection: MutableList<Wrapper<Int>>, onUI: Boolean = true) =
        ListWrapper("test", "test.ui=$onUI", onUI, collection)
}