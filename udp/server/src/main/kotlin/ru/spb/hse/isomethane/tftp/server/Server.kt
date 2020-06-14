package ru.spb.hse.isomethane.tftp.server

import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

class Server(port: Int, rootDirectory: File) {
    private val socket = DatagramSocket(port)
    private val clientContainer = ClientContainer(rootDirectory)
    private val serverThread = thread {
        val byteArray = ByteArray(Constants.PACKET_SIZE)
        val packet = DatagramPacket(byteArray, byteArray.size)
        var time: Long
        while (true) {
            if (clientContainer.isEmpty()) {
                socket.soTimeout = 0
            } else {
                socket.soTimeout = Constants.RETRANSMISSION_TIMEOUT
            }
            try {
                socket.receive(packet)
                time = System.currentTimeMillis()
                val response = clientContainer.processPacket(packet, time)
                response?.let { socket.send(it) }
            } catch (e: SocketTimeoutException) {
                time = System.currentTimeMillis()
            } catch (e: SocketException) {
                return@thread
            }
            val timedOut = clientContainer.timedOutPackets(time)
            timedOut.forEach { socket.send(it) }
            clientContainer.removeFinished()
        }
    }

    fun stop() {
        socket.close()
        serverThread.join()
    }
}
