package com.redelf.commons.extensions

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

fun <K, V> ConcurrentHashMap<K, V>.removeByValue(value: V): Boolean {

    val iterator = this.iterator()

    while (iterator.hasNext()) {

        val entry = iterator.next()

        if (entry.value == value) {

            iterator.remove()
            return true
        }
    }

    return false
}

fun <K, V> ConcurrentHashMap<K, V>.removeAllByValue(value: V): Boolean {

    var removed = false
    val iterator = this.iterator()

    while (iterator.hasNext()) {

        if (iterator.next().value == value) {

            iterator.remove()
            removed = true
        }
    }

    return removed
}

fun <T> CopyOnWriteArraySet<T>.getAtIndex(index: Int): T? {

    if (index < 0 || index >= size) {

        return null
    }

    return this.elementAt(index)
}

fun <T> CopyOnWriteArraySet<T>.removeAt(index: Int): T? {

    if (index < 0 || index >= size) return null

    val iterator = this.iterator()
    var currentIndex = 0
    var removedElement: T? = null

    while (iterator.hasNext()) {
        val element = iterator.next()
        if (currentIndex == index) {
            removedElement = element
            this.remove(element) // Thread-safe removal
            break
        }
        currentIndex++
    }

    return removedElement
}

fun <T> CopyOnWriteArraySet<T>.addAt(index: Int, element: T): Boolean {

    // First check if element exists (Set behavior)
    if (this.contains(element)) return false

    // Convert to list for index manipulation
    val tempList = this.toMutableList()

    // Handle index bounds
    when {
        index <= 0 -> tempList.add(0, element)
        index >= tempList.size -> tempList.add(element)
        else -> tempList.add(index, element)
    }

    // Clear and re-add all elements
    this.clear()
    this.addAll(tempList)

    return true
}

fun <T> CopyOnWriteArraySet<T>.sortWith(comparator: Comparator<in T>) {

    val sortedList = this.toMutableList().apply {

        sortWith(comparator)
    }

    this.clear()

    this.addAll(sortedList)
}

fun <T> CopyOnWriteArraySet<T>.contentEquals(other: CopyOnWriteArraySet<T>): Boolean {

    // Check if they're the same instance
    if (this === other) return true

    // Check size first for quick rejection
    if (this.size != other.size) return false

    // Compare elements in order using iterator
    val thisIterator = this.iterator()
    val otherIterator = other.iterator()

    while (thisIterator.hasNext() && otherIterator.hasNext()) {

        if (thisIterator.next() != otherIterator.next()) {

            return false
        }
    }

    // If we get here, all elements were equal in the same order
    return true
}

fun <T> CopyOnWriteArrayList<T>.addUnique(index: Int, element: T): Boolean {

    if (!contains(element)) {

        add(index, element)

        return true
    }

    return false
}