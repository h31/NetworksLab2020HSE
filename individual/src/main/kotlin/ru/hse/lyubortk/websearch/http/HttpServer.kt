package ru.hse.lyubortk.websearch.http

import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class HttpServer(port: Int, private val processor: RequestProcessor) {
    private val log = LoggerFactory.getLogger(HttpServer::class.java)

    private val serverSocket = ServerSocket(port)
    private val clientHandlerThreadPool = Executors.newFixedThreadPool(5)
    private val listenerThreadExecutor = Executors.newSingleThreadExecutor().also {
        it.submit(this::listenServerSocket)
    }
    private val parser = HttpMessageParser()

    // ADD TRY CATCH
    private fun listenServerSocket() {
        while (true) {
            val socket = serverSocket.accept()
            clientHandlerThreadPool.submit { processClient(socket) }
        }
    }

    // ADD TRY CATCH
    private fun processClient(socket: Socket) {
        val context = parser.createConnectionContext()
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()
        val byteArray = ByteArray(512)
        var bytesRead = inputStream.read(byteArray)
        while (bytesRead > 0) {
            when (val result = parser.parseRequests(context, byteArray.take(bytesRead))) {
                EncodingNotImplemented -> {
                    val response = processor.getNotImplementedResponse()
                    outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                    closeConnection(socket)
                    return
                }
                ParseError -> {
                    val response = processor.getBadRequestResponse()
                    outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                    closeConnection(socket)
                    return
                }
                is ParsedRequests -> {
                    for (request in result.requests) {
                        val (response, action) = processor.processRequest(request)
                        outputStream.write(HttpMessageSerializer.serialize(response).toByteArray())
                        if (action == ConnectionAction.CLOSE) {
                            closeConnection(socket)
                            return
                        }
                    }
                }
            }
            bytesRead = inputStream.read(byteArray)
        }

        // TODO 400
        closeConnection(socket)
    }

    // ADD TRY CATCH
    private fun closeConnection(socket: Socket) {
        val inputStream = socket.getInputStream()
        socket.shutdownOutput()

        val byteArray = ByteArray(512)
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
    }
}

enum class ConnectionAction {
    CLOSE,
    KEEP
}