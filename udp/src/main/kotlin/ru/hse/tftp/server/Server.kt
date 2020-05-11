package ru.hse.tftp.server

import ru.hse.tftp.DEFAULT_PORT
import ru.hse.tftp.PACKET_SIZE
import ru.hse.tftp.RESPONSE_TIMEOUT
import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import kotlin.system.exitProcess

private const val USAGE_MESSAGE = "Usage: ./gradlew run [--args=\'<port>\']"

fun main(argc: Array<String>) {
    if (argc.size > 1) {
        println(USAGE_MESSAGE)
        exitProcess(0)
    }

    val server =
        if (argc.size == 1) {
            println(argc[0])
            val port = argc[0].toIntOrNull()
            if (port == null) {
                println(USAGE_MESSAGE)
                exitProcess(0)
            }

            Server(port)
        } else {
            Server()
        }

    server.requestRoutine.start()
}

class Server(port: Int = DEFAULT_PORT) : Closeable {

    @Volatile
    private var working = true

    private val socket = try {
        DatagramSocket(port)
    } catch (e: SocketException) {
        println("Failed to connect to port $port. Exiting.")
        exitProcess(0)
    }

    val requestRoutine = Thread() {
        while (working) {
            val buffer = ByteArray(PACKET_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)

            try {
                socket.receive(packet)
            } catch (e: IOException) {
                e.printStackTrace()
                exitProcess(0)
            }

            println("Received connection packet")

            RequestHandler(packet).run()
        }
    }

    override fun close() {
        working = false
        requestRoutine.join()
        socket.close()
    }
}