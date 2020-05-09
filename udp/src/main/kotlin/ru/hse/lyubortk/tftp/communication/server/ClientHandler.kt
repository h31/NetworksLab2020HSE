package ru.hse.lyubortk.tftp.communication.server

import ru.hse.lyubortk.tftp.communication.BaseCommunicator
import ru.hse.lyubortk.tftp.communication.withConversions
import ru.hse.lyubortk.tftp.model.*
import java.io.*
import java.net.DatagramSocket
import java.net.SocketAddress

@kotlin.ExperimentalUnsignedTypes
class ClientHandler private constructor(private val clientAddress: SocketAddress) : Closeable, BaseCommunicator() {
    private fun run(request: Request, serverSocket: DatagramSocket) {
        try {
            when (request) {
                is ReadRequest -> {
                    val inputStream = File(request.fileName).inputStream().withConversions(request.mode)
                    sendData(inputStream, clientAddress)
                }
                is WriteRequest -> {
                    val file = File(request.fileName)
                    if (!file.createNewFile()) {
                        sendMessage(
                            ErrorMessage(ErrorType.FILE_ALREADY_EXISTS, FILE_ALREADY_EXISTS_MESSAGE),
                            clientAddress,
                            serverSocket
                        )
                        return
                    }
                    val outputStream = file.outputStream().withConversions(request.mode)
                    receiveData(outputStream)
                }
            }
        } catch (e: FileNotFoundException) { // happened before acknowledgment
            sendMessage(ErrorMessage(ErrorType.FILE_NOT_FOUND, e.message ?: ""), clientAddress, serverSocket)
        } catch (e: IOException) {
            if (connectionEstablished) { // we should send error from main port if something breaks BEFORE the acknowledgment
                sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""), clientAddress)
            } else {
                sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""), clientAddress, serverSocket)
            }
        }
    }

    private fun receiveData(outputStream: OutputStream) {
        outputStream.use { output ->
            var blockNumber: UShort = 1u
            do {
                val data = sendMessageWithRetry(Acknowledgment((blockNumber - 1u).toUShort()), clientAddress) {
                    (it as? Data)?.takeIf { data ->
                        data.blockNumber == blockNumber
                    }
                }?.first

                if (data == null) {
                    sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_DATA_MESSAGE), clientAddress)
                    System.err.println(NO_DATA_MESSAGE)
                    return
                }

                connectionEstablished = true
                val nonNullableData: Data = data
                output.write(nonNullableData.data)
                blockNumber++ // Overflow is totally fine!
            } while (nonNullableData.data.size == 512)
            sendMessage(Acknowledgment((blockNumber - 1u).toUShort()), clientAddress)
        }
    }

    companion object ClientHandler {
        fun start(clientAddress: SocketAddress, request: Request, serverSocket: DatagramSocket) {
            Thread {
                try {
                    ClientHandler(clientAddress).use { clientHandler ->
                        clientHandler.run(request, serverSocket)
                    }
                } catch (e: Exception) {
                    System.err.println("Unhandled exception in ClientHandler: $e")
                }
            }.start()
        }

        private const val FILE_ALREADY_EXISTS_MESSAGE = "File already exists"
    }
}