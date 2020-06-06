package ru.hse.lyubortk.websearch.http.implementation.parsing

import ru.hse.lyubortk.websearch.http.implementation.parsing.ConnectionContext.Companion.Body.ChuckedEncodingBody
import ru.hse.lyubortk.websearch.http.implementation.parsing.ConnectionContext.Companion.Body.ContentLengthBody
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*

sealed class ParseResult<out T>
object ParseError : ParseResult<Nothing>()
object EncodingNotImplemented : ParseResult<Nothing>()
data class ParsedMessages<U>(val messages: List<U>) : ParseResult<U>()

abstract class HttpMessageParser<T, U : ConnectionContext> {

    abstract fun createConnectionContext(): U

    fun parseMessages(context: U, bytes: List<Byte>): ParseResult<T> {
        context.unparsedBytes.addAll(bytes)
        context.parsedEverythingPossible = false

        val createdMessages = mutableListOf<T>()

        while (!context.parsedEverythingPossible) {
            val parseResult = when (context.state) {
                ConnectionContext.Companion.State.START_LINE -> parseStartLine(context)
                ConnectionContext.Companion.State.HEADERS -> parseHeaders(context)
                ConnectionContext.Companion.State.BODY -> parseBody(context)
            }
            when (parseResult) {
                ParseError -> return parseResult
                EncodingNotImplemented -> return parseResult
                is ParsedMessages -> createdMessages.addAll(parseResult.messages)
                null -> {
                }
            }
        }

        if (context.state != ConnectionContext.Companion.State.BODY &&
            context.unparsedBytes.size > MAX_UNPARSED_BYTES
        ) {
            return ParseError
        }
        return ParsedMessages(createdMessages)
    }

    private fun parseBody(context: U): ParseResult<T>? {
        return when (val body = context.body) {
            null -> {
                val request = createMessage(context) ?: return ParseError
                context.reset()
                ParsedMessages(listOf(request))
            }
            is ContentLengthBody -> parseContentLengthBody(body, context)
            is ChuckedEncodingBody -> parseChunkedEncodingBody(body, context)
        }
    }

    private fun parseContentLengthBody(body: ContentLengthBody, context: U): ParseResult<T>? {
        val readBytes = context.unparsedBytes.pollNBytes(body.remainingContentLength)
        body.remainingContentLength -= readBytes.size
        body.bytes.addAll(readBytes)
        if (body.remainingContentLength == 0) {
            val message = createMessage(context) ?: return ParseError
            context.reset()
            return ParsedMessages(listOf(message))
        }
        return needMoreBytes(context)
    }

    private fun parseChunkedEncodingBody(body: ChuckedEncodingBody, context: U): ParseResult<T>? {
        when (val chunkSize = body.chunkSize) {
            null -> {
                val nextLineBytes = context.unparsedBytes.pollFirstLine() ?: return needMoreBytes(context)
                try {
                    body.chunkSize = Integer.parseInt(String(nextLineBytes.toByteArray()), 16)
                } catch (e: NumberFormatException) {
                    return ParseError
                }
            }
            0 -> { // skip trailers (it is fine according to RFC) and read empty line
                val nextLineBytes = context.unparsedBytes.pollFirstLine() ?: return needMoreBytes(context)
                if (nextLineBytes.isEmpty()) {
                    val message = createMessage(context) ?: return ParseError
                    context.reset()
                    return ParsedMessages(listOf(message))
                }
            }
            else -> {
                if (context.unparsedBytes.size < chunkSize + 2) { // 2 for CRLF
                    return needMoreBytes(context)
                }
                val readBytes = context.unparsedBytes.pollNBytes(chunkSize)
                val nextLine = context.unparsedBytes.pollFirstLine()
                if (nextLine == null || nextLine.isNotEmpty()) {
                    return ParseError
                }
                body.chunkSize = null
                context.body?.bytes?.addAll(readBytes)
            }
        }
        return null
    }

    protected abstract fun parseStartLine(context: U): ParseResult<Nothing>?

    private fun parseHeaders(context: U): ParseResult<Nothing>? {
        val nextLineBytes = context.unparsedBytes.pollFirstLine() ?: return needMoreBytes(context)
        val nextLineString = String(nextLineBytes.toByteArray())
        if (nextLineString.isEmpty()) {
            context.state = ConnectionContext.Companion.State.BODY
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
        context: ConnectionContext,
        headerName: String,
        headerValue: String
    ): ParseResult<Nothing>? {
        if (!headerName.equals(TRANSFER_ENCODING_HEADER, true)) {
            return null
        }
        if (context.body != null) {
            return ParseError
        }
        if (!headerValue.equals(CHUNKED_TRANSFER_ENCODING, true)) {
            return EncodingNotImplemented
        }
        context.body = ChuckedEncodingBody(mutableListOf(), null)
        return null
    }

    private fun handleContentLength(
        context: ConnectionContext,
        headerName: String,
        headerValue: String
    ): ParseResult<Nothing>? {
        if (!headerName.equals(CONTENT_LENGTH_HEADER, true)) {
            return null
        }
        if (context.body != null) {
            return ParseError
        }
        val length = headerValue.toIntOrNull() ?: return ParseError
        context.body = ContentLengthBody(length, mutableListOf())
        return null
    }

    protected fun needMoreBytes(context: U): Nothing? {
        context.parsedEverythingPossible = true
        return null
    }

    protected abstract fun createMessage(context: U): T?

    companion object {
        private const val MAX_UNPARSED_BYTES = 10_000_000
    }
}

abstract class ConnectionContext {
    var parsedEverythingPossible = false
    var unparsedBytes: ArrayDeque<Byte> = ArrayDeque()
    var state: State = State.START_LINE

    var parsedHeaders: MutableMap<String, MutableList<String>> = TreeMap(CASE_INSENSITIVE_ORDER)
    var body: Body? = null

    open fun reset() {
        parsedEverythingPossible = false
        unparsedBytes = ArrayDeque()
        state = State.START_LINE
        parsedHeaders = TreeMap(CASE_INSENSITIVE_ORDER)
        body = null
    }

    companion object {
        enum class State {
            START_LINE,
            HEADERS,
            BODY
        }

        sealed class Body(var bytes: MutableList<Byte>) {
            class ContentLengthBody(var remainingContentLength: Int, bytes: MutableList<Byte>) : Body(bytes)
            class ChuckedEncodingBody(bytes: MutableList<Byte>, var chunkSize: Int?) : Body(bytes)
        }
    }
}