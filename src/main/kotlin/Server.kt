package ru.hse.spb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.*
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("please specify port")
        return
    }
    try {
        val port = args[0].toInt()
        val socket = DatagramSocket(port)

        mainLoop@ while (socket.isBound) {
            val bufferSize = 516
            val buffer = ByteArray(bufferSize)
            val datagramPacket = DatagramPacket(buffer, bufferSize)
            try {
                socket.receive(datagramPacket)
                val packet = parseDatagramPacket(datagramPacket)
                when (packet) {
                    is RRQPacket -> {
                        GlobalScope.launch {
                            val filename = StandardCharsets.US_ASCII.decode(packet.filename).toString()
                            System.err.println("RRQ $filename started")
                            val mode = StandardCharsets.US_ASCII.decode(packet.mode).toString()
                            val temporarySocket = DatagramSocket()
                            sendFile(filename, mode, datagramPacket.socketAddress, temporarySocket)
                            System.err.println("RRQ $filename finished")
                        }
                    }
                    is WRQPacket -> {
                        GlobalScope.launch {
                            val filename = StandardCharsets.US_ASCII.decode(packet.filename).toString()
                            System.err.println("WRQ $filename started")
                            val mode = StandardCharsets.US_ASCII.decode(packet.mode).toString()
                            val temporarySocket = DatagramSocket()
                            receiveFile(filename, mode, datagramPacket.socketAddress, temporarySocket)
                            System.err.println("WRQ $filename finished")
                        }
                    }
                    else -> continue@mainLoop
                }
            } catch (e: IOException) {
                // supress
            }

        }
    } catch (e: NumberFormatException) {
        println("please specify port as integer")
        return
    } catch (e: SocketException) {
        println("socket could not be opened, or the socket could not bind to the specified local port.")
        return
    } catch (e: SecurityException) {
        println("security exception")
        return
    }
}