package ru.hse.lyubortk.websearch.http.implementation.connector

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.http.implementation.HttpMessageSerializer
import ru.hse.lyubortk.websearch.http.implementation.parsing.EncodingNotImplemented
import ru.hse.lyubortk.websearch.http.implementation.parsing.HttpRequestParser
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParseError
import ru.hse.lyubortk.websearch.http.implementation.parsing.ParsedMessages
import ru.hse.lyubortk.websearch.http.implementation.processor.HttpServerMessageProcessor
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class HttpServerConnector(port: Int, private val processor: HttpServerMessageProcessor) : BaseConnector() {
    override val log: Logger = LoggerFactory.getLogger(HttpServerConnector::class.java)

    private val serverSocket = ServerSocket(port)
    private val clientHandlerThreadPool = Executors.newCachedThreadPool()
    private val listenerThreadExecutor = Executors.newSingleThreadExecutor().also {
        it.submit(this::listenServerSocket)
    }

    private fun listenServerSocket() {
        try {
            while (true) {
                val socket = serverSocket.accept()
                clientHandlerThreadPool.submit { processClient(socket) }
            }
        } catch (e: Exception) {
            log.error("Exception in server socket listener", e)
            throw e
        }
    }

    private fun processClient(socket: Socket) {
        try {
            socket.soTimeout = SOCKET_READ_TIMEOUT_MILLIS
            val context = HttpRequestParser.createConnectionContext()
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            val inputBuffer = ByteArray(BUFFER_SIZE)
            var bytesRead = inputStream.read(inputBuffer)
            var finished = false
            while (bytesRead > 0 && !finished) {
                when (val parseResult = HttpRequestParser.parseMessages(context, inputBuffer.take(bytesRead))) {
                    EncodingNotImplemented -> {
                        val response = processor.getNotImplementedResponse()
                        outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                        finished = true
                    }
                    ParseError -> {
                        val response = processor.getBadRequestResponse()
                        outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                        finished = true
                    }
                    is ParsedMessages -> {
                        requests@ for (request in parseResult.messages) {
                            val (response, action) = processor.processRequest(request)
                            outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                            if (action == ConnectionAction.CLOSE) {
                                finished = true
                                break@requests
                            }
                        }
                    }
                }
                if (!finished) {
                    bytesRead = inputStream.read(inputBuffer)
                }
            }
            closeConnection(socket)
        } catch (e: Exception) {
            log.error("Exception while processing client ${socket.inetAddress}", e)
            if (!socket.isClosed) {
                socket.close()
            }
            throw e
        }
    }

    companion object {
        private const val BUFFER_SIZE = 512
        private const val SOCKET_READ_TIMEOUT_MILLIS = 25_000
    }
}

enum class ConnectionAction {
    CLOSE,
    KEEP
}