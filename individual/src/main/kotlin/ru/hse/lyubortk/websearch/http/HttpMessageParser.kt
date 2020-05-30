package ru.hse.lyubortk.websearch.http

import java.util.*

class HttpMessageParser {

    fun createConnectionContext(): ConnectionContext = ConnectionContextImpl()

    fun parseRequests(context: ConnectionContext, bytes: List<Byte>): ParseResult {
        context as ConnectionContextImpl
        context.unparsedBytes.addAll(bytes)

        var hasUnparsedData = true
        val createdMessages = mutableListOf<HttpRequest>()

        while (hasUnparsedData) {
            if (context.state == ConnectionContextImpl.Companion.State.BODY) {
                when (context.bodyType) {
                    null -> {
                        val request = createHttpRequestFromContext(context) ?: return ParseError
                        createdMessages.add(request)
                        context.reset()
                    }
                    ConnectionContextImpl.Companion.BodyType.CONTENT_LENGTH -> {
                        if (context.remainingContentLength == 0) {
                            val request = createHttpRequestFromContext(context) ?: return ParseError
                            createdMessages.add(request)
                            context.reset()
                        } else {
                            val readBytes = context.unparsedBytes.take(context.remainingContentLength)
                            context.remainingContentLength -= readBytes.size
                            context.body!!.addAll(readBytes)
                        }
                    }
                    ConnectionContextImpl.Companion.BodyType.CHUNKED_ENCODING -> TODO()
                }
            } else {
                val nextLineBytes = context.unparsedBytes.pollFirstLine()
                if (nextLineBytes == null) {
                    hasUnparsedData = false
                    continue
                }
                val nextLineString = String(nextLineBytes.toByteArray())
                when (context.state) {
                    ConnectionContextImpl.Companion.State.START_LINE -> {
                        val requestLine = nextLineString.split(SPACE_CHAR)
                        if (requestLine.size != 3) {
                            return ParseError
                        }
                        context.method = requestLine[0]
                        context.requestTarget = requestLine[1]
                        context.httpVersion = requestLine[2]
                        context.state = ConnectionContextImpl.Companion.State.HEADERS
                    }
                    ConnectionContextImpl.Companion.State.HEADERS -> {
                        if (nextLineString.isEmpty()) {
                            context.state = ConnectionContextImpl.Companion.State.BODY
                        } else {
                            val colonIndex = nextLineString.indexOf(':')
                            if (colonIndex == -1) {
                                return ParseError
                            }
                            val name = nextLineString.substring(0, colonIndex)
                            val value = nextLineString.substring(colonIndex + 1).trim()

                            // RFC states that server MUST reject requests with spaces in the end of header name
                            if (name.last() == '\n') {
                                return ParseError
                            }

                            if (name.equals(TRANSFER_ENCODING_HEADER, true)) {
                                if (context.bodyType != null) {
                                    return ParseError
                                }
                                if (!name.equals(CHUNKED_TRANSFER_ENCODING, true)) {
                                    return EncodingNotImplemented
                                }
                                context.bodyType = ConnectionContextImpl.Companion.BodyType.CHUNKED_ENCODING
                                context.body = mutableListOf()
                            }

                            if (name.equals(CONTENT_LENGTH_HEADER, true)) {
                                if (context.bodyType != null) {
                                    return ParseError
                                }
                                val length = value.toIntOrNull() ?: return ParseError
                                context.bodyType = ConnectionContextImpl.Companion.BodyType.CONTENT_LENGTH
                                context.remainingContentLength = length
                                context.body = mutableListOf()
                            }

                            context.parsedHeaders.getOrPut(name, ::mutableListOf).add(value)
                        }
                    }
                    else -> error("This cannot happen")
                }
            }
        }

        if (context.state != ConnectionContextImpl.Companion.State.BODY && context.unparsedBytes.size > MAX_UNPARSED_BYTES) {
            return ParseError
        }
        return ParsedRequests(createdMessages)
    }

    private fun createHttpRequestFromContext(contextImpl: ConnectionContextImpl): HttpRequest? {
        val method = contextImpl.method
        val requestTarget = contextImpl.requestTarget
        val httpVersion = contextImpl.httpVersion
        if (method == null || requestTarget == null || httpVersion == null) {
            return null
        }
        return HttpRequest(method, requestTarget, httpVersion, contextImpl.parsedHeaders, contextImpl.body)
    }

    companion object {
        private const val TRANSFER_ENCODING_HEADER = "Transfer-Encoding"
        private const val CONTENT_LENGTH_HEADER = "Content-Length"
        private const val CHUNKED_TRANSFER_ENCODING = "chunked"
        private const val MAX_UNPARSED_BYTES = 10_000
    }
}

sealed class ParseResult
object ParseError : ParseResult()
object EncodingNotImplemented : ParseResult()
data class ParsedRequests(val requests: List<HttpRequest>) : ParseResult()

sealed class ConnectionContext
private class ConnectionContextImpl : ConnectionContext() {
    var unparsedBytes: ArrayDeque<Byte> = ArrayDeque()
    var state: State = State.START_LINE

    var method: String? = null
    var requestTarget: String? = null
    var httpVersion: String? = null

    var parsedHeaders: MutableMap<String, MutableList<String>> = mutableMapOf()

    var bodyType: BodyType? = null
    var remainingContentLength = 0

    var body: MutableList<Byte>? = null

    fun reset() {
        val other = ConnectionContextImpl()
        unparsedBytes = other.unparsedBytes
        state = other.state
        method = other.method
        requestTarget = other.requestTarget
        httpVersion = other.httpVersion
        parsedHeaders = other.parsedHeaders
        bodyType = other.bodyType
        remainingContentLength = other.remainingContentLength
        body = other.body
    }

    companion object {
        enum class State {
            START_LINE,
            HEADERS,
            BODY
        }

        enum class BodyType {
            CONTENT_LENGTH,
            CHUNKED_ENCODING
        }
    }
}

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