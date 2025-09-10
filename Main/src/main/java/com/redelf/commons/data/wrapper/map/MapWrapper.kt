package com.redelf.commons.data.wrapper.map

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MapWrapper<T> (

    from: Any,
    environment: String,

    @JsonProperty("dataMap")
    @SerializedName("dataMap")
    private var dataMap: ConcurrentHashMap<Long, T>

) {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Data map hash = ${getHashCode()} ::"

    fun put(from: String, key: Long, value: T) {

        if (DEBUG.get()) Console.log("$tag put(key=$key) from '$from'")

        dataMap[key] = value
    }

    fun get(key: Long): T? {

        return dataMap[key]
    }

    fun remove(from: String, id: Long) {

        Console.warning("$tag remove(id=$id) from '$from'")

        dataMap.remove(id)
    }

    fun clear(from: String) {

        Console.warning("$tag doClear() from '$from'")

        dataMap.clear()
    }

    fun getHashCode(): Int {

        return dataMap.hashCode()
    }

    fun getSize(): Int {

        return dataMap.size
    }

    fun toList(): CopyOnWriteArrayList<T> {

        return dataMap.values.toCollection(CopyOnWriteArrayList())
    }

    fun toKeysList(): CopyOnWriteArrayList<Long> {

        val destination = CopyOnWriteArrayList<Long>()

        destination.addAll(dataMap.keys)

        return destination
    }

    fun getDataCopy():  ConcurrentHashMap<Long, T> {

        val data = ConcurrentHashMap<Long, T>()

        data.putAll(dataMap)

        return data
    }
}