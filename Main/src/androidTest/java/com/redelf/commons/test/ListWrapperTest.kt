package com.redelf.commons.test

import com.redelf.commons.data.wrappers.ListWrapper
import org.junit.Assert
import org.junit.Test

class ListWrapperTest : BaseTest() {

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

            Assert.assertEquals(1, wrapper.get(0))
            Assert.assertEquals(3, wrapper.get(1))
            Assert.assertEquals(5, wrapper.get(2))
        }
    }

    @Test
    fun testRemoveIndex() {

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
    fun testRemoveItem() {

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
    fun testUpdate() {

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
    fun testIndexOf() {

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
    fun testRemoveAll() {

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
    fun testClear() {

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

        }


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

            wrapper.addAll( challengeData, "testAdd.all")

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

    private fun createCollection() = mutableListOf(1, 3, 5, 7, 9)

    private fun createChallengeCollection() = mutableListOf(2, 4, 6, 8, 10)

    private fun createWrapper(collection: MutableList<Int>, onUI: Boolean = true) =
        ListWrapper("test", "test.ui=$onUI", onUI, collection)

    private fun createWrappers(collection: MutableList<Int>) = listOf(

        createWrapper(collection, true ),
        createWrapper(collection, false )
    )
}