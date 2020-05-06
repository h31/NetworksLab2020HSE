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
                var readBytes: Int
                var blockNumber: Short = 1
                do {
                    readBytes = inputStream.read(bytes)
                    val successful = sendData(Data(blockNumber, bytes.copyOf(readBytes)))
                    if (!successful) {
                        sendError(ErrorMessage(ErrorType.NOT_DEFINED, NO_ACKNOWLEDGMENT_MESSAGE))
                        return
                    }
                    blockNumber++
                } while (readBytes == 512)
            }
        } catch (e: FileNotFoundException) {
            sendError(ErrorMessage(ErrorType.FILE_NOT_FOUND, e.message ?: ""))
        } catch (e: IOException) {
            sendError(ErrorMessage(ErrorType.ACCESS_VIOLATION, e.message ?: ""))
        }
    }

    private fun runWrite(writeRequest: WriteRequest) {
        TODO()
    }

    // returns true if send was successfull
    private fun sendData(data: Data): Boolean {
        val bytes = Serializer.serialize(data)
        val packetToSend = DatagramPacket(bytes, 0, bytes.size, clientAddress)

        for (i in 0 until RETRY_NUMBER) {
            socket.send(packetToSend)

            val futures = singleThreadExecutor.invokeAll(mutableListOf(Callable {
                val receivedPacket = DatagramPacket(ByteArray(TFTP_PACKET_MAX_LENGTH), TFTP_PACKET_MAX_LENGTH)
                socket.receive(receivedPacket)
                receivedPacket
            }), ACKNOWLEDGMENT_TIMOUT_MILLIS, TimeUnit.MILLISECONDS)

            if (futures[0].isCancelled) {
                continue
            }
            val message = try {
                Deserializer.deserialize(futures[0].get().data)
            } catch (e: DeserializationException) {
                continue
            }
            if (message is Acknowledgment && message.blockNumber == data.blockNumber) {
                return true
            }
        }
        return false
    }

    private fun sendError(errorMessage: ErrorMessage) {
        val bytes = Serializer.serialize(errorMessage)
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

        private const val RETRY_NUMBER = 5
        private const val ACKNOWLEDGMENT_TIMOUT_MILLIS = 3000L
        private const val NO_ACKNOWLEDGMENT_MESSAGE = "Cannot receive any acknowledgment for sent packet"
    }
}