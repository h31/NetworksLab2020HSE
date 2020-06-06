package ru.hse.lyubortk.websearch.util

import java.util.*
import kotlin.NoSuchElementException
import kotlin.random.Random

/**
 * O(log n) for all operations (cannot be achieved with default collections).
 * Not thread safe
 */
class RandomTreeSet<T> {
    private val elementToId: TreeMap<T, Int> = TreeMap()
    private val idToElement: TreeMap<Int, T> = TreeMap()

    fun add(element: T): Boolean {
        if (elementToId.containsKey(element)) {
            return false
        }
        val id = Random.nextInt()
        elementToId[element] = id
        idToElement[id] = element
        return true
    }

    fun getRandom(): T {
        val id = Random.nextInt()
        val tailMap = idToElement.tailMap(id)
        return if (tailMap.isEmpty()) {
            idToElement.lastEntry()?.value ?: throw NoSuchElementException()
        } else {
            tailMap[tailMap.firstKey()]!!
        }
    }

    fun remove(element: T): Boolean {
        if (!elementToId.containsKey(element)) {
            return false
        }
        val id = elementToId[element]
        elementToId.remove(element)
        idToElement.remove(id)
        return true
    }

    fun isEmpty(): Boolean = elementToId.isEmpty()

    fun isNotEmpty(): Boolean = elementToId.isNotEmpty()

    fun size(): Int = elementToId.size
}