package ru.hse.lyubortk.tftp.server

import ru.hse.lyubortk.tftp.TFTP_PACKET_MAX_LENGTH
import ru.hse.lyubortk.tftp.TFTP_SERVER_PORT
import ru.hse.lyubortk.tftp.model.*
import ru.hse.lyubortk.tftp.model.ErrorType.ILLEGAL_OPERATION
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress

fun main(args: Array<String>) = try {
    val port = args.getOrNull(0)?.toIntOrNull() ?: TFTP_SERVER_PORT
    println("Using port: $port")
    Server(port).use { server ->
        server.run()
    }
} catch (e: Exception) {
    System.err.println("Exception in server was not handled: $e")
}

class Server(serverPort: Int) : Closeable {
    private val socket: DatagramSocket = DatagramSocket(serverPort)

    fun run() {
        while (true) {
            val packet = DatagramPacket(ByteArray(TFTP_PACKET_MAX_LENGTH), TFTP_PACKET_MAX_LENGTH)
            socket.receive(packet)
            packet.socketAddress
            try {
                when (val message =
                    Deserializer.deserialize(packet.data.copyOf(packet.length))) {
                    is Request -> ClientHandler.start(packet.socketAddress, message)
                    else -> sendError(
                        packet.socketAddress,
                        ErrorMessage(ILLEGAL_OPERATION, WRONG_MESSAGE_TYPE_ERROR)
                    )
                }
            } catch (e: DeserializationException) {
                System.err.println("Cannot deserialize message: $e")
                sendError(
                    packet.socketAddress,
                    ErrorMessage(ILLEGAL_OPERATION, e.message ?: "")
                )
            }
        }
    }

    private fun sendError(socketAddress: SocketAddress, errorMessage: ErrorMessage) {
        val bytes = Serializer.serialize(errorMessage)
        val packet = DatagramPacket(bytes, 0, bytes.size, socketAddress)
        socket.send(packet)
    }

    override fun close() {
        socket.close()
    }

    companion object {
        private const val WRONG_MESSAGE_TYPE_ERROR = "Wrong message type sent to server port"
    }
}