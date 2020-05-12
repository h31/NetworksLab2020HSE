package ru.hse.anstkras.tftp

import ru.hse.anstkras.tftp.packet.*
import java.io.FileNotFoundException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class Server(private val port: Int) {
    fun start() {
        val socket = DatagramSocket(port)
        while (true) {
            val ackPacketSize = Client.bufferCapacity
            val buffer = ByteBuffer.allocate(Client.bufferCapacity)
            val recievedPacket = DatagramPacket(buffer.array(), ackPacketSize)
            socket.receive(recievedPacket)
            val tftpPacket = PacketParser.parsePacket(ByteBuffer.wrap(recievedPacket.data, 0, recievedPacket.length))
            val curSocket = DatagramSocket()
            try {
                when (tftpPacket) {
                    is RRQPacket -> {
                        thread(start = true) {
                            sendFile(
                                StandardCharsets.US_ASCII.decode(tftpPacket.fileName).toString(),
                                curSocket,
                                recievedPacket.socketAddress
                            )
                        }
                    }
                    is WRQPacket -> {
                        thread(start = true) {
                            AckPacket(0).send(curSocket, recievedPacket.socketAddress)
                            getFile(curSocket, StandardCharsets.US_ASCII.decode(tftpPacket.fileName).toString())
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                ErrorPacket.getFileNotFoundPacket().send(curSocket, recievedPacket.socketAddress)
            } catch (e: IOException) {
                ErrorPacket.getUndefinedErrorPacket(e.message.orEmpty()).send(curSocket, recievedPacket.socketAddress)
            }
        }
    }
}