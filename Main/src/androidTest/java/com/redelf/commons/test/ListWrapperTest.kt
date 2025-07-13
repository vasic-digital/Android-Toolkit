package com.redelf.commons.test

import com.redelf.commons.data.model.Wrapper
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

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)

        wrappers.forEachIndexed { index, wrapper ->

            wrapper.add("testAdd.$index", challengeData[0])

            val size = wrapper.getList().size
            val defaultSize = createCollection().size

            Assert.assertTrue(size > defaultSize)
        }
    }

    @Test
    fun testGet() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testRemoveIndex() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testRemoveItem() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testUpdate() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testIndexOf() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testRemoveAll() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testClear() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testReplaceAndFilter() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testAddAllAndFilter() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testAddAll() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testPurge() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


    }

    @Test
    fun testGetSize() {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrappers = createWrappers(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrappers[0].getList() == collection)
        Assert.assertTrue(wrappers[0].getSize() == collection.size)
        Assert.assertTrue(wrappers[1].getList() == collection)
        Assert.assertTrue(wrappers[1].getSize() == collection.size)


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