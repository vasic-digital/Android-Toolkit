package com.redelf.commons.data.wrapper.map

import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.get
import kotlin.text.get

class DoubleMapWrapper<T>(

    from: Any,
    environment: String,

    // TODO: Make sure that Long is generic type as T as well
    // TODO: Add all power features that ListWrapper has
    private val map: ConcurrentHashMap<Long, ConcurrentHashMap<Long, T?>?>

) {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Map hash = ${map.hashCode()} ::"

    fun getKeys(): Set<Long> {

        return map.keys
    }

    fun getMap(id: Long): ConcurrentHashMap<Long, T?> {

        var map = map[id]
        val tag = "$tag getMap(id=$id, mapHash=${map.hashCode()}') ::"

        if (map == null) {

            Console.debug("$tag Instantiate map")

            map = ConcurrentHashMap()

        } else {

            if (DEBUG.get()) Console.log("$tag Size = ${map.size}")
        }

        return map
    }

    fun putMap(id: Long, data: ConcurrentHashMap<Long, T?>?): ConcurrentHashMap<Long, T?> {

        val tag = "$tag putMap(id=$id, mapHash=${map.hashCode()}') ::"
        val map = data ?: ConcurrentHashMap()

        this@DoubleMapWrapper.map[id] = map

        if (DEBUG.get()) Console.log("$tag New map size = ${map.size}")

        return map
    }

    fun deleteById(id: Long) {

        Console.warning("$tag deleteById(): %d", id)

        map.remove(id)
    }

    fun deleteBy(id: Long, entityId: Long) {

        Console.warning("$tag deleteBy(): Chat id = %d, Entity Id = %d", id, entityId)

        val map = getMap(id)

        if (map.remove(entityId) != null) {

            if (DEBUG.get()) Console.log(

                "$tag Removed contact from the Map: %s -> %s",
                id, entityId
            )

        } else {

            Console.error(

                "Could not remove contact from the Map: %s -> %s",
                id, entityId
            )
        }
    }

    fun doClear(from: String) {

        Console.warning("$tag doClear() from '$from'")

        map.clear()
    }

    fun getHashCode(): Int {

        return map.hashCode()
    }

    fun getSize(): Int {

        return map.size
    }
}