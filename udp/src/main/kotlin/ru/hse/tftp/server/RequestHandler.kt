package ru.hse.tftp.server

import ru.hse.tftp.PATH_TO_SERVER_STORAGE
import ru.hse.tftp.RESPONSE_TIMEOUT
import ru.hse.tftp.getBytesToOutputStream
import ru.hse.tftp.packet.AcknolegmentPacket
import ru.hse.tftp.packet.ErrorType.FILE_ALREADY_EXISTS
import ru.hse.tftp.packet.ErrorType.FILE_NOT_FOUND
import ru.hse.tftp.packet.ReadRequest
import ru.hse.tftp.packet.WriteRequest
import ru.hse.tftp.packet.parse
import ru.hse.tftp.sendBytes
import ru.hse.tftp.sendErrorPacket
import ru.hse.tftp.sendPacket
import ru.hse.tftp.sendPacketWithoutAcknolegment
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class RequestHandler(packet: DatagramPacket) : Thread() {
    private val remoteSocketAddress = packet.socketAddress
    private val remoteAddress = packet.address
    private val remotePort = packet.port

    private val data: ByteArray = packet.data
    private val length = packet.length
    private val offset = packet.offset

    private val remoteInetSocketAddress = InetSocketAddress(remoteAddress, remotePort)
    private val localSocket = DatagramSocket()

    init {
        localSocket.soTimeout = RESPONSE_TIMEOUT
    }

    override fun run() {
        when (val result = parse(data, offset, length)) {
            is ReadRequest -> handleReadRequest(result)
            is WriteRequest -> handleWriteRequest(result)
            else -> {
                println("Wrong request")
            }
        }
    }

    private fun handleReadRequest(request: ReadRequest) {
        println("New read request ${request.filename}")

        val file = File(PATH_TO_SERVER_STORAGE + request.filename)

        if (!file.exists()) {
            sendErrorPacket(FILE_NOT_FOUND, remoteInetSocketAddress, localSocket)
            return
        }

        val packet = AcknolegmentPacket()
        sendPacketWithoutAcknolegment(packet, remoteInetSocketAddress, localSocket)

        sendBytes(file.inputStream(), remoteInetSocketAddress, localSocket)
    }

    private fun handleWriteRequest(request: WriteRequest) {
        println("New write request ${request.filename}")

        val file = File(PATH_TO_SERVER_STORAGE + request.filename)

        if (file.exists()) {
            sendErrorPacket(
                FILE_ALREADY_EXISTS,
                remoteInetSocketAddress, localSocket
            )

            return
        }

        val packet = AcknolegmentPacket()
        sendPacketWithoutAcknolegment(packet, remoteInetSocketAddress, localSocket)

        getBytesToOutputStream(remoteInetSocketAddress, localSocket, file.outputStream())
    }
}
