package ru.hse.lyubortk.websearch.http.implementation.connector

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.http.implementation.HttpMessageSerializer
import ru.hse.lyubortk.websearch.http.implementation.HttpRequest
import ru.hse.lyubortk.websearch.http.implementation.HttpResponse
import ru.hse.lyubortk.websearch.http.implementation.parsing.EncodingNotImplemented
import ru.hse.lyubortk.websearch.http.implementation.parsing.HttpResponseParser
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParseError
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParsedMessages
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration
import javax.net.ssl.SSLSocketFactory

class HttpClientConnector(private val connectTimeout: Duration) : BaseConnector() {
    override val log: Logger = LoggerFactory.getLogger(HttpClientConnector::class.java)

    fun sendRequest(
        request: HttpRequest,
        inetSocketAddress: InetSocketAddress,
        timeout: Duration,
        useTls: Boolean
    ): HttpResponse {
        var socket = Socket()
        try {
            socket.connect(inetSocketAddress, connectTimeout.toMillis().toInt())
            if (useTls) {
                socket = wrapInSslSocket(socket, inetSocketAddress)
            }
            socket.soTimeout = timeout.toMillis().toInt()
            socket.getOutputStream().write(HttpMessageSerializer.serialize(request).toByteArray())

            val inputStream = socket.getInputStream()
            val inputBuffer = ByteArray(BUFFER_SIZE)
            var bytesRead = inputStream.read(inputBuffer)
            var finished = false
            val context = HttpResponseParser.createConnectionContext()
            var response: HttpResponse? = null
            while (!finished && bytesRead > 0) {
                when (val parseResult = HttpResponseParser.parseMessages(context, inputBuffer.take(bytesRead))) {
                    EncodingNotImplemented -> {
                        finished = true
                    }
                    ParseError -> {
                        finished = true
                    }
                    is ParsedMessages -> {
                        if (parseResult.messages.isNotEmpty()) {
                            response = parseResult.messages.first()
                            finished = true
                        }
                    }
                }
                if (!finished) {
                    bytesRead = inputStream.read(inputBuffer)
                }
            }

            if (response == null) {
                throw RuntimeException("FAIL") // TODO: fixme
            }
            closeConnection(socket)
            return response
        } catch (e: Exception) {
            log.error("Exception in http client connector", e)
            throw e
        }
    }

    private fun wrapInSslSocket(socket: Socket, inetSocketAddress: InetSocketAddress): Socket {
        return (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(
            socket,
            inetSocketAddress.hostName,
            inetSocketAddress.port,
            true
        )
    }

    companion object {
        private const val BUFFER_SIZE = 512
    }
}