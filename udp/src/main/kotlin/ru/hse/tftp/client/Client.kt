package ru.hse.tftp.client

import ru.hse.tftp.DEFAULT_PORT
import ru.hse.tftp.RESPONSE_TIMEOUT
import ru.hse.tftp.getBytesToOutputStream
import ru.hse.tftp.packet.ReadPacket
import ru.hse.tftp.packet.WritePacket
import ru.hse.tftp.sendBytes
import ru.hse.tftp.sendPacket
import java.io.Closeable
import java.io.File
import java.lang.NumberFormatException
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.system.exitProcess

private val USAGE_MESSAGE = "Usage: ./gradlew run --args=\'<read|write> <filename> <server ip> [port]\'"

fun printUsageMessage() {
    println(USAGE_MESSAGE)
    exitProcess(0)
}

fun main(argc: Array<String>) {
    if (argc.size < 3 || argc.size > 4) {
        printUsageMessage()
    }

    val command = argc[0]
    if (command != "read" && command != "write") {
        printUsageMessage()
    }

    val filename = argc[1]

    val serverIp = argc[2]

    var port = DEFAULT_PORT

    if (argc.size == 4) {
        try {
            port = Integer.parseInt(argc[3])
        } catch (e: NumberFormatException) {
            printUsageMessage()
        }
    }

    val address = InetSocketAddress(serverIp, port)

    Client(address).use { client ->
        when (command) {
            "read" -> {
                client.read(filename)
            }

            "write" -> {
                client.write(filename)
            }
        }
    }
}

class Client(private val serverAddress: InetSocketAddress): Closeable {
    private val socket = DatagramSocket()

    init {
        socket.soTimeout = RESPONSE_TIMEOUT
    }

    fun read(name: String) {
        val packet = ReadPacket(name)
        val address = sendPacket(packet, socket, serverAddress)

        if (address != null) {
            val file = File(name)
            if (!file.exists()) {
                file.createNewFile()
            }

            val outputStream = file.outputStream()
            getBytesToOutputStream(address, socket, outputStream)
        } else {
            println("Error writing to remote server")
            exitProcess(0)
        }
    }

    fun write(filename: String) {
        val packet = WritePacket(filename)
        val address = sendPacket(packet, socket, serverAddress)

        if (address != null) {
            val inputStream = File(filename).inputStream()
            sendBytes(inputStream,  address, socket)
        }
    }

    override fun close() {
        socket.close()
    }
}