package ru.hse.anstkras.tftp.packet

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.nio.ByteBuffer

interface Packet {
    fun getBytesRepresentation(): ByteBuffer

    fun send(socket: DatagramSocket, address: SocketAddress) {
        val bytesArray = getBytesRepresentation().array()
        socket.send(DatagramPacket(bytesArray, bytesArray.size, address))
    }
}