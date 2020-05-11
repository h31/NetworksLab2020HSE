package ru.hse.anstkras.tftp

import ru.hse.anstkras.tftp.packet.RRQPacket
import java.net.*

class Client(private val host: String, private val port: Int, private val mode: TFTPMode) {
    private val inetSocketAddress = InetSocketAddress(host, port)

    fun writeFile() {

    }

    fun readFile(fileName: String) {
        val rrqPacket = RRQPacket(fileName, mode)
        val socket = DatagramSocket()
        rrqPacket.send(socket, inetSocketAddress)

    }
}

enum class TFTPMode {
    NETASCII {
        override fun modeStringRepresentation() = "netascii"


    }, OCTET {
        override fun modeStringRepresentation() = "octet"

    };


    abstract fun modeStringRepresentation(): String
}