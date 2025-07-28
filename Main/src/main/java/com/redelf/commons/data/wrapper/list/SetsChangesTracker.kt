package com.redelf.commons.data.wrapper.list

import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.ObtainParametrized
import java.io.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

class SetsChangesTracker<T, I>(

    private val context: String,
    private val set: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val setCopy: CopyOnWriteArraySet<T> = CopyOnWriteArraySet(),
    private val identifierObtainer: ObtainParametrized<I?, Int>,
    private val changedIdentifiers: CopyOnWriteArraySet<I> = CopyOnWriteArraySet<I>()

) : Runnable {

    private val copying = AtomicBoolean()

    override fun run() {

        val changes = findChangedIdentifiers()

        changedIdentifiers.clear()

        if (changes.isNotEmpty()) {

            changedIdentifiers.addAll(changes)

            Console.log("Changed identifiers :: ${changes.toList()}")
        }
    }

    fun makeCopy(from: String) {

        if (copying.get()) {

            Console.log("Set copy skip for 0 millis :: Context='$context', From='$from'")

            return
        }

        copying.set(true)

        val start = System.currentTimeMillis()

        setCopy.clear()

        set.forEach { item ->

            try {

                val c = deepCopyItem(item)

                setCopy.add(c)

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        val time = System.currentTimeMillis() - start

        Console.log("Set copy made for $time millis :: Context='$context', From='$from'")

        copying.set(false)
    }

    fun findChangedIdentifiers(): Set<I> {

        val changedIdentifiers = mutableSetOf<I>()

        set.forEachIndexed { index, currentItem ->

            val lastItem = setCopy.elementAtOrNull(index)

            if (!itemsEqual(currentItem, lastItem)) {

                identifierObtainer.obtain(index)?.let {

                    changedIdentifiers.add(it)
                }
            }
        }

        if (setCopy.size > set.size) {

            (set.size until setCopy.size).forEach {

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

    fun addItem(item: T) = set.add(item)

    fun removeItem(item: T) = set.remove(item)

    fun getItems(): List<T> = set.toList()

    fun clear() {

        set.clear()
        setCopy.clear()
        changedIdentifiers.clear()
    }
}
