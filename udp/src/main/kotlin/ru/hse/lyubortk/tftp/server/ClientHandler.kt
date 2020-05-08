package ru.hse.lyubortk.tftp.server

import ru.hse.lyubortk.tftp.TFTP_DATA_MAX_LENGTH
import ru.hse.lyubortk.tftp.TFTP_PACKET_MAX_LENGTH
import ru.hse.lyubortk.tftp.model.*
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

@kotlin.ExperimentalUnsignedTypes
class ClientHandler private constructor(private val clientAddress: SocketAddress) : Closeable {
    private val socket: DatagramSocket = DatagramSocket()
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    private fun run(initialRequest: Request) = when (initialRequest) {
        is ReadRequest -> runRead(initialRequest)
        is WriteRequest -> runWrite(initialRequest)
    }

    private fun runRead(readRequest: ReadRequest) {
        try {
            File(readRequest.fileName).inputStream().use { inputStream ->
                val bytes = ByteArray(TFTP_DATA_MAX_LENGTH)
                var blockNumber: UShort = 1u
                do {
                    val readBytes = max(0, inputStream.read(bytes))
                    val acknowledgment = sendMessageWithRetry(Data(blockNumber, bytes.copyOf(readBytes))) {
                        (it as? Acknowledgment)?.takeIf { acknowledgment ->
                            acknowledgment.blockNumber == blockNumber
                        }
                    }
                    if (acknowledgment == null) {
                        sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_ACKNOWLEDGMENT_MESSAGE))
                        return
                    }
                    blockNumber++ // Overflow is totally fine!
                } while (readBytes == 512)
            }
        } catch (e: FileNotFoundException) {
            sendMessage(ErrorMessage(ErrorType.FILE_NOT_FOUND, e.message ?: ""))
        } catch (e: IOException) {
            sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""))
        }
    }

    private fun runWrite(writeRequest: WriteRequest) {
        try {
            val file = File(writeRequest.fileName)
            if (!file.createNewFile()) {
                sendMessage(ErrorMessage(ErrorType.FILE_ALREADY_EXISTS, FILE_ALREADY_EXISTS_MESSAGE))
                return
            }
            file.outputStream().use { outputStream ->
                var blockNumber: UShort = 1u
                do {
                    val data = sendMessageWithRetry(Acknowledgment((blockNumber - 1u).toUShort())) {
                        (it as? Data)?.takeIf { acknowledgment ->
                            acknowledgment.blockNumber == blockNumber
                        }
                    }
                    if (data == null) {
                        sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_DATA_MESSAGE))
                        return
                    }
                    val nonNullableData: Data = data
                    outputStream.write(nonNullableData.data)
                    blockNumber++ // Overflow is totally fine!
                } while (nonNullableData.data.size == 512)
                sendMessage(Acknowledgment((blockNumber - 1u).toUShort()))
            }
        } catch (e: IOException) {
            sendMessage(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""))
        }
    }

    // returns true if send was successful
    private fun <T : Message> sendMessageWithRetry(message: Message, responseValidation: (Message) -> T?): T? {
        val startTime = System.currentTimeMillis()
        fun getRemainingTime() = max(0, startTime + RESPONSE_TIMEOUT_MILLIS - System.currentTimeMillis())

        while (getRemainingTime() > 0) {
            sendMessage(message)

            val futures = singleThreadExecutor.invokeAll(mutableListOf(Callable {
                val receivedPacket = DatagramPacket(ByteArray(TFTP_PACKET_MAX_LENGTH), TFTP_PACKET_MAX_LENGTH)
                socket.receive(receivedPacket)
                receivedPacket
            }), getRemainingTime(), TimeUnit.MILLISECONDS)

            if (futures[0].isCancelled) {
                continue
            }
            val receivedMessage = try {
                val packet = futures[0].get()
                Deserializer.deserialize(packet.data.copyOf(packet.length))
            } catch (e: DeserializationException) {
                continue
            }
            val validatedMessage = responseValidation(receivedMessage)
            if (validatedMessage != null) {
                return validatedMessage
            }
        }
        return null
    }

    private fun sendMessage(message: Message) {
        val bytes = Serializer.serialize(message)
        val packet = DatagramPacket(bytes, 0, bytes.size, clientAddress)
        socket.send(packet)
    }

    override fun close() {
        socket.close()
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

        private const val RESPONSE_TIMEOUT_MILLIS = 15000L
        private const val NO_ACKNOWLEDGMENT_MESSAGE = "Did not receive any acknowledgment for sent packet"
        private const val NO_DATA_MESSAGE = "Did not receive any data"
        private const val FILE_ALREADY_EXISTS_MESSAGE = "File already exists"
    }
}