package ru.hse.lyubortk.websearch.http.implementation.connector

import org.slf4j.Logger
import java.net.Socket
import java.time.Duration

abstract class BaseConnector(private val halfCloseTimeout: Duration) {
    abstract val log: Logger

    protected fun closeConnection(socket: Socket) {
        val inputStream = socket.getInputStream()
        socket.shutdownOutput()

        val byteArray = ByteArray(BUFFER_SIZE)
        val startTime = System.currentTimeMillis()
        var bytesRead = inputStream.read(byteArray)
        while (bytesRead > 0 && (System.currentTimeMillis() - startTime < halfCloseTimeout.toMillis())) {
            bytesRead = inputStream.read(byteArray)
        }
        if (bytesRead > 0) {
            log.warn("Connection with ${socket.inetAddress} is closed due to timeout before half-close")
        }
        socket.close()
    }

    companion object {
        private const val BUFFER_SIZE = 1024
    }
}