package ru.spbau.smirnov.tftp.server

import com.beust.jcommander.JCommander
import java.lang.System.exit
import java.net.*
import kotlin.system.exitProcess

class Server(
    val rootPath: String,
    serverPort: Int
) {

    private val bufferSize: Int = 516
    private val socket: DatagramSocket

    init {
        try {
            socket = DatagramSocket(serverPort)
        } catch (e: BindException) {
            println("Connecting to port $serverPort error: ${e.message}")
            exitProcess(0)
        }
    }

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
    val arguments = Arguments()
    JCommander.newBuilder()
        .addObject(arguments)
        .build()
        .parse(*args)

    val server = Server(
        arguments.rootPath,
        arguments.port
    )

    Thread { server.start() }.start()
    while (true) {
        readLine() ?: break
    }
    server.shutdown()
}