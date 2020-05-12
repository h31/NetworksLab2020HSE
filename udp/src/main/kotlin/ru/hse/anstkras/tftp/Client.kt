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
        val wrqPacket = WRQPacket(ByteBuffer.wrap(fileName.toByteArray()), mode)
        val socket = DatagramSocket()
        wrqPacket.send(socket, inetSocketAddress)
        val tftpPacket = Packet.getPacket(socket)
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
        val rrqPacket = RRQPacket(ByteBuffer.wrap(fileName.toByteArray()), mode)
        val socket = DatagramSocket()
        rrqPacket.send(socket, inetSocketAddress)
        getFile(socket, fileName)
    }

    companion object {
        val timeout = 1000
        val bufferCapacity = 1024
        val tftpPackageSize = 512
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
        val resultBuffer = ByteBuffer.allocate(Client.bufferCapacity)
        socket.soTimeout = Client.timeout
        var blockNum = 1
        while (!socket.isClosed) {
            val dataPacketSize = 516
            val buffer = ByteBuffer.allocate(Client.bufferCapacity)
            val recievedPacket = DatagramPacket(buffer.array(), dataPacketSize)
            try {
                socket.receive(recievedPacket)
            } catch (e: SocketTimeoutException) {
                error("Timeout")
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
                if (dataSize == Client.tftpPackageSize) {
                    blockNum++
                } else {
                    resultBuffer.flip()
                    val file = File(fileName)
                    file.writeBytes(ByteArray(resultBuffer.remaining()))
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
        var blockNum = 1
        val buffer = ByteBuffer.wrap(fileBytes)
        while (!socket.isClosed) {
            val bytesToSendSize = min(Client.tftpPackageSize, buffer.remaining())
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
                if (bytesToSendSize < Client.tftpPackageSize) {
                    return
                }
                blockNum++
            }
        }
    } catch (e: IOException) {
        // TODO handle this
    }
}
