package ru.hse.anstkras.tftp.packet

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
        fun getPacket(socket : DatagramSocket) : Packet {
            val ackPacketSize = 1024 // TODO size
            val buffer = ByteBuffer.allocate(1024) // TODO choose capacity
            val recievedPacket = DatagramPacket(buffer.array(), ackPacketSize)
            socket.soTimeout = 1024 // TODO
            try {
                socket.receive(recievedPacket)
            } catch (e: SocketTimeoutException) {
                error("Timeout") // TODO retransmit last package
            }
            val tftpPacket = PacketParser.parsePacket(ByteBuffer.wrap(recievedPacket.data, 0, recievedPacket.length))
            return tftpPacket
        }
    }
}