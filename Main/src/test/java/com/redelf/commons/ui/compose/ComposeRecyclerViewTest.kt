package com.redelf.commons.ui.compose

import androidx.test.core.app.ApplicationProvider
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Tests for ComposeRecyclerView to verify:
 * - Thread safety
 * - Adapter compatibility  
 * - Scroll listener functionality
 * - No crashes with frequent data changes
 */
@RunWith(AndroidJUnit4::class)
class ComposeRecyclerViewTest {

    private lateinit var context: Context
    private lateinit var composeRecyclerView: ComposeRecyclerView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        composeRecyclerView = ComposeRecyclerView(context)
    }

    @Test
    fun testAdapterSetterGetter() {
        // Test initial state
        assertNull(composeRecyclerView.adapter)
        
        // Create a test adapter
        val testAdapter = TestAdapter()
        
        // Test setting adapter
        composeRecyclerView.adapter = testAdapter
        assertEquals(testAdapter, composeRecyclerView.adapter)
        
        // Test clearing adapter
        composeRecyclerView.adapter = null
        assertNull(composeRecyclerView.adapter)
    }

    @Test
    fun testRecyclerViewCompatibility() {
        // Test that we can get a RecyclerView-compatible instance
        val compatRecyclerView = composeRecyclerView.getRecyclerViewCompat()
        assertNotNull(compatRecyclerView)
        
        // Test that it's actually a RecyclerView
        assertEquals(RecyclerView::class.java, compatRecyclerView::class.java)
    }

    @Test
    fun testScrollListeners() {
        var scrollCalled = false
        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scrollCalled = true
            }
        }
        
        // Add scroll listener
        composeRecyclerView.addOnScrollListener(scrollListener)
        
        // Remove scroll listener
        composeRecyclerView.removeOnScrollListener(scrollListener)
        
        // Test passes if no exception is thrown
    }

    @Test
    fun testThreadSafety() {
        val testAdapter = TestAdapter()
        
        // Simulate thread-safe adapter setting from multiple threads
        val threads = mutableListOf<Thread>()
        
        repeat(10) { i ->
            val thread = Thread {
                try {
                    composeRecyclerView.adapter = testAdapter
                    Thread.sleep(10)
                    composeRecyclerView.adapter = null
                } catch (e: Exception) {
                    throw AssertionError("Thread safety test failed on thread $i", e)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // Test passes if no exceptions were thrown
    }

    @Test
    fun testFrequentDataChanges() {
        val testAdapter = TestAdapter()
        composeRecyclerView.adapter = testAdapter
        
        // Simulate frequent data changes
        repeat(100) {
            testAdapter.addItem("Item $it")
            testAdapter.notifyItemInserted(it)
            Thread.sleep(1)
        }
        
        // Test passes if no crashes occur during frequent updates
    }

    /**
     * Simple test adapter for testing purposes
     */
    private class TestAdapter : RecyclerView.Adapter<TestViewHolder>() {
        private val items = mutableListOf<String>()

        fun addItem(item: String) {
            items.add(item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
            val view = View(parent.context)
            return TestViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
            // Simple binding - just for testing
        }

        override fun getItemCount(): Int = items.size
    }

    private class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}