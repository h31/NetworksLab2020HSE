package ru.hse.lyubortk.websearch.http.implementation

import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.http.GetResponse
import ru.hse.lyubortk.websearch.http.HttpClient
import ru.hse.lyubortk.websearch.http.implementation.parsing.*
import java.lang.Exception
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.time.Duration

class HttpClientImpl(private val connectTimeout: Duration) : HttpClient {
    private val log = LoggerFactory.getLogger(HttpClientImpl::class.java)

    override fun get(uri: URI, timeout: Duration): GetResponse {
        val socket = Socket()
        try {
            val host = uri.host
            val port = uri.port.let { if (it == -1) 80 else it }
            val path = uri.path?.ifEmpty { "/" } ?: "/"
            val query = uri.query ?: ""

            val request = HttpRequest(
                GET,
                path + query,
                HTTP_VERSION,
                mapOf(
                    HOST_HEADER to listOf(host),
                    CONNECTION_HEADER to listOf(CONNECTION_CLOSE_VALUE)
                ),
                null
            )

            socket.connect(InetSocketAddress(host, port), connectTimeout.toMillis().toInt())
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
                throw RuntimeException("FAIL")
            }
            closeConnection(socket)
            return object : GetResponse {
                override fun responseUri(): URI = uri
                override fun headers(): Map<String, List<String>> = response.headers
                override fun body(): String? = response.body?.toByteArray()?.let{ String(it) } // never throws
            }
        } catch (e: Exception) {
            log.error("Exception in http client GET method", e)
            if (!socket.isClosed) {
                socket.close()
            }
            throw e
        }
    }

    private fun closeConnection(socket: Socket) {
        val inputStream = socket.getInputStream()
        socket.shutdownOutput()

        val byteArray = ByteArray(BUFFER_SIZE)
        val startTime = System.currentTimeMillis()
        var bytesRead = inputStream.read(byteArray)
        while (bytesRead > 0 && (System.currentTimeMillis() - startTime < HALF_CLOSE_TIMEOUT_MILLIS)) {
            bytesRead = inputStream.read(byteArray)
        }
        if (bytesRead > 0) {
            log.warn("Connection from ${socket.inetAddress} is closed due to timeout before half-close")
        }
        socket.close()
    }

    companion object {
        private const val GET = "GET"
        private const val HTTP_VERSION = "HTTP/1.1"
        private const val HOST_HEADER = "Host"
        private const val CONNECTION_HEADER = "Connection"
        private const val CONNECTION_CLOSE_VALUE = "close"
        private const val BUFFER_SIZE = 512
        private const val HALF_CLOSE_TIMEOUT_MILLIS = 10_000
    }
}