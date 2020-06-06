package ru.hse.lyubortk.websearch.http.implementation.parsing

import ru.hse.lyubortk.websearch.http.implementation.HttpResponse
import ru.hse.lyubortk.websearch.http.implementation.parsing.ResponseConnectionContext.Companion.StatusLine

object HttpResponseParser : HttpMessageParser<HttpResponse, ResponseConnectionContext>() {
    override fun createConnectionContext(): ResponseConnectionContext = ResponseConnectionContext()

    override fun parseStartLine(context: ResponseConnectionContext): ParseResult<Nothing>? {
        val nextLineBytes = context.unparsedBytes.pollFirstLine()
        if (nextLineBytes == null) {
            context.parsedEverythingPossible = true
            return null
        }
        val nextLineString = String(nextLineBytes.toByteArray())
        val requestLine = nextLineString.split(SPACE_CHAR, limit = 3)
        if (requestLine.size != 3) {
            return ParseError
        }
        val statusCode = requestLine[1].toIntOrNull() ?: return ParseError
        context.statusLine = StatusLine(requestLine[0], statusCode, requestLine[2])
        context.state = ConnectionContext.Companion.State.HEADERS
        return null
    }

    override fun createMessage(context: ResponseConnectionContext): HttpResponse? {
        val statusLine = context.statusLine ?: return null
        val body = context.body?.bytes
        return HttpResponse(
            statusLine.httpVersion,
            statusLine.statusCode,
            statusLine.reasonPhrase,
            context.parsedHeaders,
            body
        )
    }
}

class ResponseConnectionContext : ConnectionContext() {
    var statusLine: StatusLine? = null

    override fun reset() {
        super.reset()
        statusLine = null
    }

    companion object {
        data class StatusLine(
            val httpVersion: String,
            val statusCode: Int,
            val reasonPhrase: String
        )
    }
}