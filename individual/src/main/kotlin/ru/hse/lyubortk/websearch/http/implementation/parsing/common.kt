package ru.hse.lyubortk.websearch.http.implementation.parsing

import java.util.*

const val TRANSFER_ENCODING_HEADER = "Transfer-Encoding"
const val CONTENT_LENGTH_HEADER = "Content-Length"
const val CHUNKED_TRANSFER_ENCODING = "chunked"

const val CR: Byte = '\r'.toByte()
const val LF: Byte = '\n'.toByte()
const val SPACE_CHAR = ' '

fun ArrayDeque<Byte>.pollFirstLine(): List<Byte>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var lastIndex = -1 // last expected CR candidate index
    var found = false
    var currentElement = iterator.next()
    while (iterator.hasNext() && !found) {
        val nextElement = iterator.next()
        if (currentElement == CR && nextElement == LF) {
            found = true
        }
        lastIndex++
        currentElement = nextElement
    }
    if (!found) {
        return null
    }
    return List(lastIndex + 2) { this.pollFirst() }.dropLast(2)
}

fun ArrayDeque<Byte>.pollNBytes(n: Int): List<Byte> {
    val result = this.take(n)
    for (i in result.indices) {
        this.poll()
    }
    return result
}