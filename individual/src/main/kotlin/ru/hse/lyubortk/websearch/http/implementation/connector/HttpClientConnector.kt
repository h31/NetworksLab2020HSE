package ru.hse.lyubortk.websearch.http.implementation.connector

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.config.ClientConnectorConfig
import ru.hse.lyubortk.websearch.http.implementation.*
import ru.hse.lyubortk.websearch.http.implementation.parsing.EncodingNotImplemented
import ru.hse.lyubortk.websearch.http.implementation.parsing.HttpResponseParser
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParseError
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParsedMessages
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration
import javax.net.ssl.SSLSocketFactory

class HttpClientConnector(private val config: ClientConnectorConfig) : BaseConnector(config.halfCloseTimeout) {

    override val log: Logger = LoggerFactory.getLogger(HttpClientConnector::class.java)

    fun sendRequest(
        request: HttpRequest,
        inetSocketAddress: InetSocketAddress,
        timeout: Duration,
        useTls: Boolean
    ): HttpResponse {
        var socket = Socket()
        try {
            socket.connect(inetSocketAddress, config.connectTimeout.toMillis().toInt())
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
            var exception: Exception? = null
            while (!finished && bytesRead > 0) {
                when (val parseResult = HttpResponseParser.parseMessages(context, inputBuffer.take(bytesRead))) {
                    EncodingNotImplemented -> {
                        finished = true
                        exception = UnsupportedEncodingException(UNSUPPORTED_ENCODING_MESSAGE)
                    }
                    ParseError -> {
                        finished = true
                        exception = ResponseParseErrorException(PARSE_ERROR_MESSAGE)
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

            closeConnection(socket)
            if (exception != null) {
                throw exception
            }
            return response!!
        } catch (e: Exception) {
            if (!socket.isClosed) {
                socket.close()
            }
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
        private const val UNSUPPORTED_ENCODING_MESSAGE = "Host replied with message in unsupported encoding"
        private const val PARSE_ERROR_MESSAGE = "Cannot parse http response"
        private const val BUFFER_SIZE = 1024
    }
}