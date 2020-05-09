package ru.hse.lyubortk.tftp.communication.server

import ru.hse.lyubortk.tftp.TFTP_DATA_MAX_LENGTH
import ru.hse.lyubortk.tftp.communication.BaseCommunicator
import ru.hse.lyubortk.tftp.communication.withConversions
import ru.hse.lyubortk.tftp.model.*
import java.io.*
import java.net.InetSocketAddress

@kotlin.ExperimentalUnsignedTypes
class ClientHandler private constructor(private val clientAddress: InetSocketAddress) : Closeable, BaseCommunicator() {
    private fun run(request: Request) {
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
                            clientAddress
                        )
                        return
                    }
                    val outputStream = file.outputStream().withConversions(request.mode)
                    receiveData(outputStream)
                }
            }
        } catch (e: FileNotFoundException) {
            sendMessage(ErrorMessage(ErrorType.FILE_NOT_FOUND, e.message ?: ""), clientAddress)
        } catch (e: IOException) {
            sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""), clientAddress)
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

                val nonNullableData: Data = data
                output.write(nonNullableData.data)
                blockNumber++ // Overflow is totally fine!
            } while (nonNullableData.data.size == TFTP_DATA_MAX_LENGTH)
            sendMessage(Acknowledgment((blockNumber - 1u).toUShort()), clientAddress)
        }
    }

    companion object ClientHandler {
        fun start(clientAddress: InetSocketAddress, request: Request) {
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