@file:Suppress("UNCHECKED_CAST")

package com.redelf.commons.data

import androidx.room.concurrent.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

object MemoryStorage {

    val DEBUG = AtomicBoolean()

    private val data = ConcurrentHashMap<String, Any?>()

    fun <T> put(key: String, value: T?): Boolean {

        data[key] = value

        return true
    }

    fun <T> get(key: String): T? {

        return data[key] as T?
    }

    fun <T> get(key: String, defaultValue: T): T {

        return data[key] as T? ?: defaultValue
    }

    fun delete(key: String): Boolean {

        return data.remove(key) != null
    }

    fun deleteAll(): Boolean {

        data.clear()
        return true
    }

    fun contains(key: String): Boolean {

        return data.containsKey(key)
    }
}
