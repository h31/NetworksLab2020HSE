package ru.hse.lyubortk.tftp.communication.server

import ru.hse.lyubortk.tftp.communication.BaseCommunicator
import ru.hse.lyubortk.tftp.communication.withConversions
import ru.hse.lyubortk.tftp.model.*
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketAddress

@kotlin.ExperimentalUnsignedTypes
class ClientHandler private constructor(clientAddress: SocketAddress) : Closeable, BaseCommunicator(clientAddress) {
    private fun run(request: Request) {
        try {
            when (request) {
                is ReadRequest -> {
                    val inputStream = File(request.fileName).inputStream().withConversions(request.mode)
                    sendData(inputStream)
                }
                is WriteRequest -> {
                    val file = File(request.fileName)
                    if (!file.createNewFile()) {
                        sendMessage(ErrorMessage(ErrorType.FILE_ALREADY_EXISTS, FILE_ALREADY_EXISTS_MESSAGE))
                        return
                    }
                    val outputStream = file.outputStream().withConversions(request.mode)
                    receiveData(outputStream, Acknowledgment(0.toUShort()))
                }
            }
        } catch (e: FileNotFoundException) {
            sendMessage(ErrorMessage(ErrorType.FILE_NOT_FOUND, e.message ?: ""))
        } catch (e: IOException) {
            sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""))
        }
    }

    companion object ClientHandler {
        fun start(clientAddress: SocketAddress, request: Request) {
            Thread {
                try {
                    ClientHandler(clientAddress).use { clientHandler ->
                        clientHandler.run(request)
                    }
                } catch (e: Exception) {
                    System.err.println("Unhandled exception in ClientHandler: $e")
                }
            }.start()
        }

        private const val FILE_ALREADY_EXISTS_MESSAGE = "File already exists"
    }
}