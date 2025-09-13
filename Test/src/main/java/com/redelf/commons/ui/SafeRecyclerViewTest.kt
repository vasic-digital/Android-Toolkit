package com.redelf.commons.ui

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for SafeRecyclerView to ensure it handles edge cases without crashing
 */
@RunWith(AndroidJUnit4::class)
class SafeRecyclerViewTest {

    private lateinit var context: Context
    private lateinit var collectionView: CollectionView
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        collectionView = CollectionView(context)
    }

    @Test
    fun testSafeRecyclerViewInitialization() {
        assertNotNull("SafeRecyclerView should be initialized", collectionView)
        assertTrue("Initial item count should be 0", collectionView.getSafeItemCount() == 0)
    }

    @Test
    fun testSafeItemCount() {
        // Test with null adapter
        assertEquals("Safe item count should be 0 with null adapter", 0, collectionView.getSafeItemCount())
        
        // Test with mock adapter
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private var itemCount = 5
            
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = itemCount
        }
        
        collectionView.adapter = mockAdapter
        assertEquals("Safe item count should match adapter item count", 5, collectionView.getSafeItemCount())
    }

    @Test
    fun testIsValidPosition() {
        // Test with null adapter
        assertFalse("Position should be invalid with null adapter", collectionView.isValidPosition(0))
        
        // Test with mock adapter
        val mockAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.widget.TextView(context)) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 3
        }
        
        collectionView.adapter = mockAdapter
        
        assertTrue("Position 0 should be valid", collectionView.isValidPosition(0))
        assertTrue("Position 1 should be valid", collectionView.isValidPosition(1))
        assertTrue("Position 2 should be valid", collectionView.isValidPosition(2))
        assertFalse("Position 3 should be invalid", collectionView.isValidPosition(3))
        assertFalse("Negative position should be invalid", collectionView.isValidPosition(-1))
    }

    @Test
    fun testSafeLayoutManager() {
        // Test setting layout manager doesn't crash
        collectionView.layoutManager = LinearLayoutManager(context)
        assertNotNull("Layout manager should be set", collectionView.layoutManager)
        
        // Test setting null layout manager
        collectionView.layoutManager = null
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
        
        collectionView.layoutManager = LinearLayoutManager(context)
        collectionView.adapter = mockAdapter
        
        // Test safe scroll operations - should not crash
        collectionView.scrollToPosition(5)
        collectionView.smoothScrollToPosition(3)
        collectionView.scrollToPosition(-1) // Invalid position - should not crash
        collectionView.scrollToPosition(100) // Out of bounds - should not crash
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
        
        collectionView.adapter = mockAdapter
        
        // Test safe refresh operations - should not crash
        collectionView.safeRefresh()
        collectionView.safeNotifyDataSetChanged()
        collectionView.safeForcedLayout()
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
        
        collectionView.adapter = mockAdapter
        
        // Test cleanup doesn't crash
        collectionView.cleanup()
        
        // After cleanup, operations should be safe
        assertEquals("Item count should be 0 after cleanup", 0, collectionView.getSafeItemCount())
        assertFalse("Position should be invalid after cleanup", collectionView.isValidPosition(0))
    }
}