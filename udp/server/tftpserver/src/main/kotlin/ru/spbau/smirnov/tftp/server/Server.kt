package ru.spbau.smirnov.tftp.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class Server(
    val rootPath: String = "",
    serverPort: Int = 69,
    private val bufferSize: Int = 516
) {
    private val socket = DatagramSocket(serverPort)
    @Volatile private var isRunning = true
    private val activeClients = mutableSetOf<Pair<InetAddress, Int>>()

    fun start() {
        val buffer = ByteArray(bufferSize)

        while (isRunning) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch(e: SocketException) {
                if (isRunning) {
                    println("Server socket was closed.\n${e.message}")
                }
                break
            }

            if ((packet.address to packet.port) in activeClients) {
                // just ignore
                continue
            }

            val message = PacketParser.parsePacket(packet)
            val connection =
                Connection(packet.address, packet.port, this, message)
            activeClients.add(packet.address to packet.port)

            connection.start()
        }
        println("Server is shutting down...")
    }

    /**
     * Closes server socket and marks server as not running.
     *
     * Does not interrupt active connections. Server will finish when all of them finish their execution
     */
    fun shutdown() {
        isRunning = false
        socket.close()
    }

    @Synchronized
    fun finishConnection(address: InetAddress, port: Int) {
        activeClients.remove(address to port)
    }
}

fun main(args: Array<String>) {
    val server = Server(serverPort = 6973)
    Thread { server.start() }.start()
    while (true) {
        readLine() ?: break
    }
    server.shutdown()
}
