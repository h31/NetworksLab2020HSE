package ru.hse.anstkras.tftp

import ru.hse.anstkras.tftp.packet.*
import java.io.File
import java.io.IOException
import java.lang.Integer.min
import java.net.*
import java.nio.ByteBuffer

class Client(private val host: String, private val port: Int, private val mode: TFTPMode) {
    private val inetSocketAddress = InetSocketAddress(host, port)

    fun writeFile(fileName: String) {
        val wrqPacket = WRQPacket(ByteBuffer.wrap(fileName.toByteArray()), mode) // TODO charset
        val socket = DatagramSocket()
        wrqPacket.send(socket, inetSocketAddress)
        val tftpPacket = Packet.getPacket(socket)
        if (tftpPacket !is AckPacket) {
            error("error") // TODO
        } else {
            sendFile(fileName, socket, inetSocketAddress)
        }
    }

    fun readFile(fileName: String) {
        val rrqPacket = RRQPacket(ByteBuffer.wrap(fileName.toByteArray()), mode) // TODO charset
        val socket = DatagramSocket()
        rrqPacket.send(socket, inetSocketAddress)
        getFile(socket, fileName)
    }
}

enum class TFTPMode(val tftpName: String) {
    NETASCII("netascii") {
        override fun modeStringRepresentation() = "netascii"


    },
    OCTET("octet") {
        override fun modeStringRepresentation() = "octet"

    };

    abstract fun modeStringRepresentation(): String
}

fun getFile(socket: DatagramSocket, fileName: String) {
    try {
        val resultBuffer = ByteBuffer.allocate(1024) // TODO
        socket.soTimeout = 1024 // TODO choose this properly
        var blockNum = 1
        while (!socket.isClosed) {
            val dataPacketSize = 516
            val buffer = ByteBuffer.allocate(1024) // TODO choose capacity
            val recievedPacket = DatagramPacket(buffer.array(), dataPacketSize)
            try {
                socket.receive(recievedPacket)
            } catch (e: SocketTimeoutException) {
                error("Timeout") // TODO retransmit last package
            }
            val tftpPacket = PacketParser.parsePacket(ByteBuffer.wrap(recievedPacket.data, 0, recievedPacket.length))
            if (tftpPacket is ErrorPacket) {
                System.err.println(tftpPacket.getErrorMessage())
                return
            }
            if (tftpPacket is DataPacket) {
                if (blockNum != tftpPacket.blockNum) {
                    // TODO handle this
                    return
                }

                val ackPacket = AckPacket(tftpPacket.blockNum)
                ackPacket.send(socket, recievedPacket.socketAddress)
                val dataSize = tftpPacket.data.remaining()
                resultBuffer.put(tftpPacket.data)
                if (dataSize == 512) { // TODO magic const
                    blockNum++
                } else {
                    resultBuffer.flip()
                    val file = File(fileName)
                    file.writeBytes(ByteArray(resultBuffer.remaining())) // TODO charsets
                }
            }
        }

    } catch (e: IOException) {
        // TODO handle this
    }
}

fun sendFile(fileName: String, socket: DatagramSocket, socketAddress: SocketAddress) {
    try {
        val fileBytes = File(fileName).readBytes()
        // TODO charsets
        var blockNum = 1
        val buffer = ByteBuffer.wrap(fileBytes)
        while (!socket.isClosed) {
            val bytesToSendSize = min(512, buffer.remaining())
            val byteArrayToSend = ByteArray(bytesToSendSize)
            buffer.get(byteArrayToSend, 0, bytesToSendSize)
            val dataPacket = DataPacket(blockNum, ByteBuffer.wrap(byteArrayToSend))
            dataPacket.send(socket, socketAddress)
            val tftpPacket = Packet.getPacket(socket)
            if (tftpPacket is ErrorPacket) {
                System.err.println(tftpPacket.getErrorMessage())
                return
            }
            if (tftpPacket is AckPacket) {
                if (bytesToSendSize < 512) { // TODO
                    return
                }
                blockNum++
            }
            // TODO handle timeout
        }
    } catch (e: IOException) {
        // TODO handle
    }
}
