package com.redelf.commons.data.wrapper.list

import com.redelf.commons.logging.Console
import java.io.*
import java.util.concurrent.CopyOnWriteArraySet

class ListComparator<T>(

    private val list: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val lastCopy: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val changedIndexes: CopyOnWriteArraySet<Int> = CopyOnWriteArraySet<Int>()

) : Runnable {

    override fun run() {

        val changes = findChangedIndexes()

        changedIndexes.clear()

        if (changes.isNotEmpty()) {

            changedIndexes.addAll(changes)

            Console.log("Changed indexes :: ${changes.toList()}")
        }
    }

    fun makeCopy() {

        lastCopy.clear()

        list.forEach { item ->

            lastCopy.add(deepCopyItem(item))
        }
    }

    fun findChangedIndexes(): Set<Int> {

        val changedIndexes = mutableSetOf<Int>()

        list.forEachIndexed { index, currentItem ->

            val lastItem = lastCopy.elementAtOrNull(index)

            if (!itemsEqual(currentItem, lastItem)) {

                changedIndexes.add(index)
            }
        }

        if (lastCopy.size > list.size) {

            (list.size until lastCopy.size).forEach { changedIndexes.add(it) }
        }

        return changedIndexes
    }

    @Suppress("UNCHECKED_CAST")
    private fun deepCopyItem(item: T): T? {

        if (item == null) {

            return null
        }

        return when {

            item is Serializable -> deepCopyViaSerialization(item)

            item::class.java.declaredMethods.any { it.name == "clone" } -> {

                try {

                    item::class.java.getDeclaredMethod("clone").apply {

                        isAccessible = true

                    }.invoke(item) as T

                } catch (e: Exception) {

                    throw IllegalStateException("Clone failed for ${item.javaClass.name}", e)
                }
            }

            else -> throw IllegalStateException(

                "Cannot deep copy ${item.javaClass.name} - must implement Serializable or have clone() method"
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S : Serializable> deepCopyViaSerialization(obj: S): S {

        return try {

            ByteArrayOutputStream().use { bos ->

                ObjectOutputStream(bos).use { oos ->

                    oos.writeObject(obj)
                    oos.flush()

                    ByteArrayInputStream(bos.toByteArray()).use { bis ->

                        ObjectInputStream(bis).readObject() as S
                    }
                }
            }

        } catch (e: Throwable) {

            throw IllegalStateException("Serialization copy failed", e)
        }
    }

    private fun itemsEqual(a: T?, b: T?): Boolean {

        return when {

            a === b -> true

            a == null || b == null -> false

            else -> a == b
        }
    }

    fun addItem(item: T) = list.add(item)

    fun removeItem(item: T) = list.remove(item)

    fun getItems(): List<T> = list.toList()

    fun clear() {

        list.clear()
        lastCopy.clear()
        changedIndexes.clear()
    }
}
