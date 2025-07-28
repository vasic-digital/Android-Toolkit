package com.redelf.commons.data.wrapper.list

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.ObtainParametrized
import java.io.*
import java.util.concurrent.CopyOnWriteArraySet

class ListComparator<T, I>(

    private val list: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val lastCopy: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val identifierObtainer: ObtainParametrized<I?, Int>,
    private val changedIdentifiers: CopyOnWriteArraySet<I> = CopyOnWriteArraySet<I>()

) : Runnable {

    override fun run() {

        val changes = findChangedIdentifiers()

        changedIdentifiers.clear()

        if (changes.isNotEmpty()) {

            changedIdentifiers.addAll(changes)

            Console.log("Changed identifiers :: ${changes.toList()}")
        }
    }

    fun makeCopy() {

        lastCopy.clear()

        list.forEach { item ->

            try {

                val c = deepCopyItem(item)

                lastCopy.add(c)

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }

    fun findChangedIdentifiers(): Set<I> {

        val changedIdentifiers = mutableSetOf<I>()

        list.forEachIndexed { index, currentItem ->

            val lastItem = lastCopy.elementAtOrNull(index)

            if (!itemsEqual(currentItem, lastItem)) {

                identifierObtainer.obtain(index)?.let {

                    changedIdentifiers.add(it)
                }
            }
        }

        if (lastCopy.size > list.size) {

            (list.size until lastCopy.size).forEach {

                identifierObtainer.obtain(it)?.let {

                    changedIdentifiers.add(it)
                }
            }
        }

        return changedIdentifiers
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class)
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
    @Throws(IllegalStateException::class)
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
        changedIdentifiers.clear()
    }
}
