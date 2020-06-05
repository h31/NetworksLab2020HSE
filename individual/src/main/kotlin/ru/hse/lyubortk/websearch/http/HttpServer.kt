package ru.hse.lyubortk.websearch.http

import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class HttpServer(port: Int, private val processor: RequestProcessor) {
    private val log = LoggerFactory.getLogger(HttpServer::class.java)

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
            val context = HttpMessageParser.createConnectionContext()
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            val byteArray = ByteArray(BUFFER_SIZE)
            var bytesRead = inputStream.read(byteArray)
            var finished = false
            while (bytesRead > 0 && !finished) {
                when (val parseResult = HttpMessageParser.parseRequests(context, byteArray.take(bytesRead))) {
                    EncodingNotImplemented -> {
                        val response = processor.getNotImplementedResponse()
                        outputStream.write(HttpResponseSerializer.serialize(response).toByteArray())
                        finished = true
                    }
                    ParseError -> {
                        val response = processor.getBadRequestResponse()
                        outputStream.write(HttpResponseSerializer.serialize(response).toByteArray())
                        finished = true
                    }
                    is ParsedRequests -> {
                        requests@ for (request in parseResult.requests) {
                            val (response, action) = processor.processRequest(request)
                            outputStream.write(HttpResponseSerializer.serialize(response).toByteArray())
                            if (action == ConnectionAction.CLOSE) {
                                finished = true
                                break@requests
                            }
                        }
                    }
                }
                if (!finished) {
                    bytesRead = inputStream.read(byteArray)
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
        private const val HALF_CLOSE_TIMEOUT_MILLIS = 10_000
        private const val BUFFER_SIZE = 512
        private const val SOCKET_READ_TIMEOUT_MILLIS = 25_000
    }
}

enum class ConnectionAction {
    CLOSE,
    KEEP
}