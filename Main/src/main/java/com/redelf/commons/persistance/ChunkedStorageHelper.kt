package com.redelf.commons.persistance

import android.content.SharedPreferences
import com.redelf.commons.logging.Console

/**
 * Helper class to store and retrieve large strings in SharedPreferences by chunking them.
 * Android SharedPreferences has practical limits on string size, so we split large strings
 * into smaller chunks.
 */
object ChunkedStorageHelper {

    private const val CHUNK_SIZE = 100000 // 100KB chunks to be safe
    private const val CHUNK_COUNT_SUFFIX = "_chunk_count"
    private const val CHUNK_DATA_SUFFIX = "_chunk_"

    /**
     * Stores a potentially large string by splitting it into chunks if necessary.
     */
    fun putChunkedString(preferences: SharedPreferences, key: String, value: String?): Boolean {
        return try {
            val editor = preferences.edit()

            // First, clean up any existing chunks for this key
            cleanupChunks(preferences, key)

            if (value == null || value.isEmpty()) {
                editor.putString(key, value)
                editor.apply()
                return true
            }

            if (value.length <= CHUNK_SIZE) {
                // Small enough to store directly
                editor.putString(key, value)
                editor.putInt(key + CHUNK_COUNT_SUFFIX, 0) // 0 means no chunking
                editor.apply()
                return true
            }

            // Need to chunk the data
            val chunks = value.chunked(CHUNK_SIZE)
            val chunkCount = chunks.size

            Console.log("ChunkedStorageHelper :: Storing $chunkCount chunks for key: $key")

            // Store chunk count
            editor.putInt(key + CHUNK_COUNT_SUFFIX, chunkCount)

            // Store each chunk
            chunks.forEachIndexed { index, chunk ->
                editor.putString(key + CHUNK_DATA_SUFFIX + index, chunk)
            }

            // Clear the main key to indicate chunked storage
            editor.remove(key)
            editor.apply()
            true

        } catch (e: Throwable) {
            Console.error("ChunkedStorageHelper :: Failed to store chunked data for key: $key", e)
            false
        }
    }

    /**
     * Retrieves a potentially chunked string from SharedPreferences.
     */
    fun getChunkedString(preferences: SharedPreferences, key: String): String? {
        return try {
            // Check if data is chunked
            val chunkCount = preferences.getInt(key + CHUNK_COUNT_SUFFIX, -1)

            when {
                chunkCount == -1 -> {
                    // No chunk count stored, data doesn't exist
                    null
                }
                chunkCount == 0 -> {
                    // Data was small enough to store directly
                    preferences.getString(key, null)
                }
                chunkCount > 0 -> {
                    // Data was chunked, reconstruct it
                    Console.log("ChunkedStorageHelper :: Retrieving $chunkCount chunks for key: $key")

                    val stringBuilder = StringBuilder()
                    for (i in 0 until chunkCount) {
                        val chunk = preferences.getString(key + CHUNK_DATA_SUFFIX + i, null)
                        if (chunk == null) {
                            Console.error("ChunkedStorageHelper :: Missing chunk $i for key: $key")
                            return null
                        }
                        stringBuilder.append(chunk)
                    }
                    stringBuilder.toString()
                }
                else -> {
                    Console.error("ChunkedStorageHelper :: Invalid chunk count $chunkCount for key: $key")
                    null
                }
            }
        } catch (e: Throwable) {
            Console.error("ChunkedStorageHelper :: Failed to retrieve chunked data for key: $key", e)
            null
        }
    }

    /**
     * Removes all chunks associated with a key.
     */
    fun removeChunkedString(preferences: SharedPreferences, key: String): Boolean {
        return try {
            val editor = preferences.edit()
            cleanupChunks(preferences, key, editor)
            editor.apply()
            true
        } catch (e: Throwable) {
            Console.error("ChunkedStorageHelper :: Failed to remove chunked data for key: $key", e)
            false
        }
    }

    /**
     * Cleans up any existing chunks for a key.
     */
    private fun cleanupChunks(
        preferences: SharedPreferences,
        key: String,
        editor: SharedPreferences.Editor? = null
    ) {
        val edit = editor ?: preferences.edit()

        // Get current chunk count if any
        val chunkCount = preferences.getInt(key + CHUNK_COUNT_SUFFIX, 0)

        // Remove all chunks
        if (chunkCount > 0) {
            for (i in 0 until chunkCount) {
                edit.remove(key + CHUNK_DATA_SUFFIX + i)
            }
        }

        // Remove metadata
        edit.remove(key + CHUNK_COUNT_SUFFIX)
        edit.remove(key)

        if (editor == null) {
            edit.apply()
        }
    }
}