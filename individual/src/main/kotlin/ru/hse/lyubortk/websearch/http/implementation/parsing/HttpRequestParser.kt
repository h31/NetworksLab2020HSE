package ru.hse.lyubortk.websearch.http.implementation.parsing

import ru.hse.lyubortk.websearch.http.implementation.HttpRequest
import ru.hse.lyubortk.websearch.http.implementation.parsing.RequestConnectionContext.Companion.RequestLine

object HttpRequestParser : HttpMessageParser<HttpRequest, RequestConnectionContext>() {
    override fun createConnectionContext(): RequestConnectionContext = RequestConnectionContext()

    override fun parseStartLine(context: RequestConnectionContext): ParseResult<Nothing>? {
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
        context.requestLine = RequestLine(requestLine[0], requestLine[1], requestLine[2])
        context.state = ConnectionContext.Companion.State.HEADERS
        return null
    }

    override fun createMessage(context: RequestConnectionContext): HttpRequest? {
        val requestLine = context.requestLine ?: return null
        val body = context.body?.bytes
        return HttpRequest(
            requestLine.method,
            requestLine.requestTarget,
            requestLine.httpVersion,
            context.parsedHeaders,
            body
        )
    }
}

class RequestConnectionContext : ConnectionContext() {
    var requestLine: RequestLine? = null

    override fun reset() {
        super.reset()
        requestLine = null
    }

    companion object {
        data class RequestLine(
            val method: String,
            val requestTarget: String,
            val httpVersion: String
        )
    }
}