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

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testGet() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testRemoveIndex() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testRemoveItem() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testUpdate() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testIndexOf() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testRemoveAll() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testClear() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testReplaceAndFilter() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testAddAllAndFilter() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testAddAll() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testPurge() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    @Test
    fun testGetSize() {

        val testData = instantiateTestData()
        val collection = testData[0]
        val challengeData = testData[1]
        val wrapper = testData[2]


    }

    private fun createCollection() = mutableListOf(1, 3, 5, 7, 9)

    private fun createChallengeCollection() = mutableListOf(2, 4, 6, 8, 10)

    private fun createWrapper(collection: MutableList<Int>) =
        ListWrapper("test", "test", true, collection)

    private fun instantiateTestData(): List<Any> {

        val collection = createCollection()
        val challengeData = createChallengeCollection()
        val wrapper = createWrapper(collection)

        Assert.assertTrue(collection.isNotEmpty())
        Assert.assertTrue(challengeData.isNotEmpty())
        Assert.assertTrue(collection.size == challengeData.size)
        Assert.assertTrue(wrapper.getList() == collection)
        Assert.assertTrue(wrapper.getSize() == collection.size)

        return listOf(

            collection, challengeData, wrapper
        )
    }
}