package ru.hse.spb

import java.io.File
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.math.min

fun sendPacket(packet: Packet, socket: DatagramSocket, address: SocketAddress) {
    val bytes = toByteBuffer(packet).array()
    socket.send(DatagramPacket(bytes, bytes.size, address))
}

@ExperimentalUnsignedTypes
fun waitAckPacket(blockNumber: UShort, socket: DatagramSocket, soTimeout: Int): Packet {
    while (true) { // try to get ackPacket
        val ackDatagramBytes = ByteArray(516)
        val ackDatagramPacket = DatagramPacket(ackDatagramBytes, 516)
        socket.soTimeout = soTimeout // A TFTP implementation MUST use an adaptive timeout.
        try {
            socket.receive(ackDatagramPacket)
        } catch (e: SocketTimeoutException) {
            return Packet()
        }
        val ackPacket = parseDatagramPacket(ackDatagramPacket)
        if (ackPacket is AckPacket) {
            if (ackPacket.blockNumber == blockNumber) {
                ackPacket.socketAddress = ackDatagramPacket.socketAddress
                return ackPacket
            }
        } else if (ackPacket is ErrorPacket) {
            System.err.println("Error " + ackPacket.errorCode + " " + ackPacket.errMsg)
            return ackPacket
        }
    }
}

fun readFileAsTextUsingInputStream(fileName: String) = File(fileName).inputStream().readBytes()

@ExperimentalUnsignedTypes
fun sendFile(filename: String, mode: String, socketAddress: SocketAddress, sendSocket: DatagramSocket) {
    try {
        val bytes = readFileAsTextUsingInputStream(filename)
        when {
            mode == "octet" -> {
                // nothing
            }
            mode.toLowerCase() == "netascii" -> {
                // nothing or transform bytes
            }
            else -> {
                throw IOException("Unsupported mode")
            }
        }
        val buffer = ByteBuffer.wrap(bytes, 0, bytes.size)
        var blockNumber: UShort = 1.toUShort()
        mainLoop@ while (sendSocket.isBound) {
            val packetSize = min(buffer.remaining(), 512)
            val answerByteArray = ByteArray(packetSize)
            buffer.get(answerByteArray, 0, packetSize)
            val packetData = ByteBuffer.wrap(answerByteArray, 0, packetSize)
            val dataPacket = DataPacket(blockNumber, packetData)
            var soTimeout = 1000
            while (soTimeout < 10 * 1000) {
                sendPacket(dataPacket, sendSocket, socketAddress)
                System.err.println("Sending file")
                when (val packet = waitAckPacket(blockNumber, sendSocket, soTimeout)) {
                    is AckPacket -> {
                        if (packetSize < 512) {
                            System.err.println("File has sent")
                            return
                        }
                        System.err.println("RRQ $filename DataPacket ${packet.blockNumber} has sent")
                        blockNumber = blockNumber.inc()
                        continue@mainLoop
                    }
                    is ErrorPacket -> {
                        System.err.println(
                            "Error " + packet.errorCode + " " + StandardCharsets.US_ASCII.decode(packet.errMsg)
                                .toString()
                        )
                        return
                    }
                }
                soTimeout *= 2
            }
            // timeout failed
            System.err.println("sendFile timeout failed")
            throw IOException("Timeout Failed")
        }
    } catch (e: IOException) {
        println("IOException while sending file: " + e.localizedMessage)
        val errorPacket =
            ErrorPacket(0.toUShort(), ByteBuffer.wrap(e.localizedMessage.toByteArray(StandardCharsets.US_ASCII)))
        val responseBytes = toByteBuffer(errorPacket).array()
        val responsePacket = DatagramPacket(responseBytes, responseBytes.size, socketAddress)
        sendSocket.send(responsePacket)
        sendSocket.close()
    }
}

@ExperimentalUnsignedTypes
fun receiveFile(
    filename: String,
    mode: String,
    socketAddress: SocketAddress,
    socket: DatagramSocket,
    shouldAckZero: Boolean = true
) {
    if (shouldAckZero) {
        sendPacket(AckPacket(0.toUShort()), socket, socketAddress)
    }
    try {
        val fileBuffer = ByteBuffer.allocate(512 * 512)
        var blockNumber: UShort = 1.toUShort()
        val soTimeout = 5000
        while (socket.isBound) {
            val buffer = ByteBuffer.allocate(516)
            val receivePacket = DatagramPacket(buffer.array(), 516)
            socket.soTimeout = soTimeout
            try {
                socket.receive(receivePacket)
            } catch (e: SocketTimeoutException) {
                System.err.println("WRQ $filename timeout failed")
                throw IOException("Timeout failed")
            }
            val dataPacket = parseDatagramPacket(receivePacket)
            if (dataPacket is DataPacket) {
                if (dataPacket.blockNumber == blockNumber) {
                    System.err.println("WRQ $filename DataPacket ${dataPacket.blockNumber} received")
                    sendPacket(AckPacket(dataPacket.blockNumber), socket, receivePacket.socketAddress)
                    val data = dataPacket.data
                    val dataSize = data.remaining()
                    fileBuffer.put(data)
                    if (dataSize < 512) {
                        // write file
                        break
                    }
                    blockNumber = blockNumber.inc()
                } else if (dataPacket.blockNumber.toUShort() < blockNumber.toUShort()) {
                    sendPacket(AckPacket(dataPacket.blockNumber), socket, receivePacket.socketAddress)
                }
            } else if (dataPacket is ErrorPacket) {
                System.err.println(
                    "Error " + dataPacket.errorCode + " " + StandardCharsets.US_ASCII.decode(dataPacket.errMsg)
                        .toString()
                )
                return
            }
        }
        fileBuffer.flip()
        val bytes = ByteArray(fileBuffer.remaining())
        fileBuffer.get(bytes)
        when {
            mode == "octet" -> {
                File(filename).writeBytes(bytes)
            }
            mode.toLowerCase() == "netascii" -> {
                File(filename).writeBytes(bytes) // ???
            }
            else -> {
                throw IOException("Unsupported mode")
            }
        }
    } catch (e: IOException) {
        println("IOException while receiving file: " + e.localizedMessage)
        val errorPacket =
            ErrorPacket(0.toUShort(), ByteBuffer.wrap(e.localizedMessage.toByteArray(StandardCharsets.US_ASCII)))
        val responseBytes = toByteBuffer(errorPacket).array()
        val responsePacket = DatagramPacket(responseBytes, responseBytes.size, socketAddress)
        socket.send(responsePacket)
        socket.close()
    }
}