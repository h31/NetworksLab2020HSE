package ru.hse.lyubortk.websearch.http.implementation

import java.util.*

object HttpMessageParser {

    fun createConnectionContext(): ConnectionContext = ConnectionContextImpl()

    fun parseRequests(context: ConnectionContext, bytes: List<Byte>): ParseResult {
        context as ConnectionContextImpl
        context.unparsedBytes.addAll(bytes)
        context.parsedEverythingPossible = false

        val createdMessages = mutableListOf<HttpRequest>()

        while (!context.parsedEverythingPossible) {
            val parseResult = when (context.state) {
                ConnectionContextImpl.Companion.State.START_LINE -> parseStartLine(context)
                ConnectionContextImpl.Companion.State.HEADERS -> parseHeaders(context)
                ConnectionContextImpl.Companion.State.BODY -> parseBody(context)
            }
            when (parseResult) {
                ParseError -> return parseResult
                EncodingNotImplemented -> return parseResult
                is ParsedRequests -> createdMessages.addAll(parseResult.requests)
                null -> {
                }
            }
        }

        if (context.state != ConnectionContextImpl.Companion.State.BODY &&
            context.unparsedBytes.size > MAX_UNPARSED_BYTES
        ) {
            return ParseError
        }
        return ParsedRequests(createdMessages)
    }

    private fun parseBody(context: ConnectionContextImpl): ParseResult? {
        when (context.bodyType) {
            null -> {
                val request = createHttpRequestFromContext(context) ?: return ParseError
                context.reset()
                return ParsedRequests(listOf(request))
            }
            ConnectionContextImpl.Companion.BodyType.CONTENT_LENGTH -> {
                val readBytes = context.unparsedBytes.take(context.remainingContentLength)
                context.remainingContentLength -= readBytes.size
                context.body!!.addAll(readBytes)
                if (context.remainingContentLength == 0) {
                    val request = createHttpRequestFromContext(context) ?: return ParseError
                    context.reset()
                    return ParsedRequests(listOf(request))
                }
                context.parsedEverythingPossible = true
            }
            ConnectionContextImpl.Companion.BodyType.CHUNKED_ENCODING -> TODO()
        }
        return null
    }

    private fun parseStartLine(context: ConnectionContextImpl): ParseResult? {
        val nextLineBytes = context.unparsedBytes.pollFirstLine()
        if (nextLineBytes == null) {
            context.parsedEverythingPossible = true
            return null
        }
        val nextLineString = String(nextLineBytes.toByteArray())
        val requestLine = nextLineString.split(SPACE_CHAR)
        if (requestLine.size != 3) {
            return ParseError
        }
        context.method = requestLine[0]
        context.requestTarget = requestLine[1]
        context.httpVersion = requestLine[2]
        context.state = ConnectionContextImpl.Companion.State.HEADERS
        return null
    }

    private fun parseHeaders(context: ConnectionContextImpl): ParseResult? {
        val nextLineBytes = context.unparsedBytes.pollFirstLine()
        if (nextLineBytes == null) {
            context.parsedEverythingPossible = true
            return null
        }
        val nextLineString = String(nextLineBytes.toByteArray())
        if (nextLineString.isEmpty()) {
            context.state = ConnectionContextImpl.Companion.State.BODY
            return null
        }

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

        context.parsedHeaders.getOrPut(name, ::mutableListOf).add(value)
        handleTransferEncoding(context, name, value)?.also { return@parseHeaders (it) }
        handleContentLength(context, name, value)?.also { return@parseHeaders (it) }
        return null
    }

    private fun handleTransferEncoding(
        context: ConnectionContextImpl,
        headerName: String,
        headerValue: String
    ): ParseResult? {
        if (!headerName.equals(TRANSFER_ENCODING_HEADER, true)) {
            return null
        }
        if (context.bodyType != null) {
            return ParseError
        }
        if (!headerValue.equals(CHUNKED_TRANSFER_ENCODING, true)) {
            return EncodingNotImplemented
        }
        context.bodyType = ConnectionContextImpl.Companion.BodyType.CHUNKED_ENCODING
        context.body = mutableListOf()
        return null
    }

    private fun handleContentLength(
        context: ConnectionContextImpl,
        headerName: String,
        headerValue: String
    ): ParseResult? {
        if (!headerName.equals(CONTENT_LENGTH_HEADER, true)) {
            return null
        }
        if (context.bodyType != null) {
            return ParseError
        }
        val length = headerValue.toIntOrNull() ?: return ParseError
        context.bodyType = ConnectionContextImpl.Companion.BodyType.CONTENT_LENGTH
        context.remainingContentLength = length
        context.body = mutableListOf()
        return null
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

    private fun ArrayDeque<Byte>.pollFirstLine(): List<Byte>? {
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

    private const val TRANSFER_ENCODING_HEADER = "Transfer-Encoding"
    private const val CONTENT_LENGTH_HEADER = "Content-Length"
    private const val CHUNKED_TRANSFER_ENCODING = "chunked"
    private const val MAX_UNPARSED_BYTES = 10_000

    private const val CR: Byte = '\r'.toByte()
    private const val LF: Byte = '\n'.toByte()
    private const val SPACE_CHAR = ' '
}

sealed class ParseResult
object ParseError : ParseResult()
object EncodingNotImplemented : ParseResult()
data class ParsedRequests(val requests: List<HttpRequest>) : ParseResult()

sealed class ConnectionContext
private class ConnectionContextImpl : ConnectionContext() {
    var parsedEverythingPossible = false
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
        parsedEverythingPossible = other.parsedEverythingPossible
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