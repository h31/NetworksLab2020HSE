package ru.hse.anstkras.tftp

import ru.hse.anstkras.tftp.packet.*
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

const val TIMEOUT = 10000
const val TFTP_DATA_LENGTH = 512
const val MAX_PACKET_SIZE = 4 + TFTP_DATA_LENGTH

enum class TFTPMode(val tftpName: String) {
    NETASCII("netascii") {
        override fun modeStringRepresentation() = "netascii"
    },
    OCTET("octet") {
        override fun modeStringRepresentation() = "octet"
    };

    abstract fun modeStringRepresentation(): String
}

fun getFile(socket: DatagramSocket, fileName: String) {
    val resultBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE)
    socket.soTimeout = TIMEOUT
    var blockNum = 1
    while (!socket.isClosed) {
        val dataPacketSize = MAX_PACKET_SIZE
        val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)
        val recievedPacket = DatagramPacket(buffer.array(), dataPacketSize)
        try {
            socket.receive(recievedPacket)
        } catch (e: SocketTimeoutException) {
            error("Timeout")
        }
        val tftpPacket = PacketParser.parsePacket(ByteBuffer.wrap(recievedPacket.data, 0, recievedPacket.length))
        if (tftpPacket is ErrorPacket) {
            System.err.println(tftpPacket.getErrorMessage())
            return
        }
        if (tftpPacket is DataPacket) {
            if (blockNum > tftpPacket.blockNum) {
                AckPacket(tftpPacket.blockNum).send(socket, recievedPacket.socketAddress)
                continue
            }
            if (blockNum < tftpPacket.blockNum) {
                error("Unexpected block number")
            }

            val ackPacket = AckPacket(tftpPacket.blockNum)
            ackPacket.send(socket, recievedPacket.socketAddress)
            val dataSize = tftpPacket.data.remaining()
            resultBuffer.put(tftpPacket.data)
            if (dataSize == TFTP_DATA_LENGTH) {
                blockNum++
            } else {
                resultBuffer.flip()
                val file = File(fileName)
                val fileBytes = ByteArray(resultBuffer.remaining())
                resultBuffer.get(fileBytes)
                file.writeBytes(fileBytes)
                break
            }
        }
    }
}

fun sendFile(fileName: String, socket: DatagramSocket, socketAddress: SocketAddress) {
    val fileBytes = File(fileName).readBytes()
    var blockNum = 1
    val buffer = ByteBuffer.wrap(fileBytes)
    while (!socket.isClosed) {
        val bytesToSendSize = Integer.min(TFTP_DATA_LENGTH, buffer.remaining())
        val byteArrayToSend = ByteArray(bytesToSendSize)
        buffer.get(byteArrayToSend, 0, bytesToSendSize)
        val dataPacket = DataPacket(blockNum, ByteBuffer.wrap(byteArrayToSend))
        dataPacket.send(socket, socketAddress)
        val tftpPacket = Packet.getPacket(socket, TIMEOUT)
        if (tftpPacket is ErrorPacket) {
            System.err.println(tftpPacket.getErrorMessage())
            return
        }
        if (tftpPacket is AckPacket) {
            if (bytesToSendSize < TFTP_DATA_LENGTH) {
                return
            }
            blockNum++
        }
    }
}
