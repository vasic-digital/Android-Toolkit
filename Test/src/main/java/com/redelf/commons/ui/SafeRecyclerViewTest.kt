package com.redelf.commons.ui

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Tests for SafeRecyclerView to ensure it handles edge cases without crashing
 */
@RunWith(AndroidJUnit4::class)
class SafeRecyclerViewTest {

    private lateinit var context: Context
    private lateinit var safeRecyclerView: SafeRecyclerView
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        safeRecyclerView = SafeRecyclerView(context)
    }

    @Test
    fun testSafeRecyclerViewInitialization() {
        assertNotNull("SafeRecyclerView should be initialized", safeRecyclerView)
        assertTrue("Initial item count should be 0", safeRecyclerView.getSafeItemCount() == 0)
    }

    @Test
    fun testSafeItemCount() {
        // Test with null adapter
        assertEquals("Safe item count should be 0 with null adapter", 0, safeRecyclerView.getSafeItemCount())
        
        // Test with mock adapter
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private var itemCount = 5
            
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = itemCount
        }
        
        safeRecyclerView.adapter = mockAdapter
        assertEquals("Safe item count should match adapter item count", 5, safeRecyclerView.getSafeItemCount())
    }

    @Test
    fun testIsValidPosition() {
        // Test with null adapter
        assertFalse("Position should be invalid with null adapter", safeRecyclerView.isValidPosition(0))
        
        // Test with mock adapter
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 3
        }
        
        safeRecyclerView.adapter = mockAdapter
        
        assertTrue("Position 0 should be valid", safeRecyclerView.isValidPosition(0))
        assertTrue("Position 1 should be valid", safeRecyclerView.isValidPosition(1))
        assertTrue("Position 2 should be valid", safeRecyclerView.isValidPosition(2))
        assertFalse("Position 3 should be invalid", safeRecyclerView.isValidPosition(3))
        assertFalse("Negative position should be invalid", safeRecyclerView.isValidPosition(-1))
    }

    @Test
    fun testSafeLayoutManager() {
        // Test setting layout manager doesn't crash
        safeRecyclerView.layoutManager = LinearLayoutManager(context)
        assertNotNull("Layout manager should be set", safeRecyclerView.layoutManager)
        
        // Test setting null layout manager
        safeRecyclerView.layoutManager = null
        // Should not crash
    }

    @Test
    fun testSafeScrollOperations() {
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 10
        }
        
        safeRecyclerView.layoutManager = LinearLayoutManager(context)
        safeRecyclerView.adapter = mockAdapter
        
        // Test safe scroll operations - should not crash
        safeRecyclerView.scrollToPosition(5)
        safeRecyclerView.smoothScrollToPosition(3)
        safeRecyclerView.scrollToPosition(-1) // Invalid position - should not crash
        safeRecyclerView.scrollToPosition(100) // Out of bounds - should not crash
    }

    @Test
    fun testSafeRefresh() {
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 5
        }
        
        safeRecyclerView.adapter = mockAdapter
        
        // Test safe refresh operations - should not crash
        safeRecyclerView.safeRefresh()
        safeRecyclerView.safeNotifyDataSetChanged()
        safeRecyclerView.safeForcedLayout()
    }

    @Test
    fun testCleanup() {
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 5
        }
        
        safeRecyclerView.adapter = mockAdapter
        
        // Test cleanup doesn't crash
        safeRecyclerView.cleanup()
        
        // After cleanup, operations should be safe
        assertEquals("Item count should be 0 after cleanup", 0, safeRecyclerView.getSafeItemCount())
        assertFalse("Position should be invalid after cleanup", safeRecyclerView.isValidPosition(0))
    }
}