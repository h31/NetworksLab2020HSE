package ru.hse.anstkras.tftp.packet

import ru.hse.anstkras.tftp.MAX_PACKET_SIZE
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

interface Packet {
    fun getBytesRepresentation(): ByteBuffer

    fun send(socket: DatagramSocket, address: SocketAddress) {
        val bytesArray = getBytesRepresentation().array()
        socket.send(DatagramPacket(bytesArray, bytesArray.size, address))
    }

    companion object {
        fun getPacket(socket: DatagramSocket, timeout: Int): Packet {
            val ackPacketSize = MAX_PACKET_SIZE
            val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)
            val recievedPacket = DatagramPacket(buffer.array(), ackPacketSize)
            socket.soTimeout = timeout
            try {
                socket.receive(recievedPacket)
            } catch (e: SocketTimeoutException) {
                error("Timeout")
            }
            val tftpPacket = PacketParser.parsePacket(ByteBuffer.wrap(recievedPacket.data, 0, recievedPacket.length))
            return tftpPacket
        }
    }
}