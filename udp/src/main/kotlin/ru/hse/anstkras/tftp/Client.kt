package ru.hse.anstkras.tftp

import ru.hse.anstkras.tftp.packet.*
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class Client(private val host: String, private val port: Int, private val mode: TFTPMode) {
    private val inetSocketAddress = InetSocketAddress(host, port)

    fun writeFile(fileName: String) {
        val wrqPacket = WRQPacket(StandardCharsets.US_ASCII.encode(fileName), mode)
        val socket = DatagramSocket()
        wrqPacket.send(socket, inetSocketAddress)
        val tftpPacket = Packet.getPacket(socket, TIMEOUT)
        if (tftpPacket !is AckPacket) {
            if (tftpPacket is ErrorPacket) {
                System.err.println(tftpPacket.getErrorMessage())
            } else {
                System.err.println("Wrong type of packet received")
            }
            return
        } else {
            sendFile(fileName, socket, inetSocketAddress)
        }
    }

    fun readFile(fileName: String) {
        val rrqPacket = RRQPacket(StandardCharsets.US_ASCII.encode(fileName), mode)
        val socket = DatagramSocket()
        rrqPacket.send(socket, inetSocketAddress)
        getFile(socket, fileName)
    }
}