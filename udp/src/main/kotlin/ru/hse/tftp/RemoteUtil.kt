package ru.hse.tftp

import ru.hse.tftp.packet.Acknolegment
import ru.hse.tftp.packet.AcknolegmentPacket
import ru.hse.tftp.packet.Data
import ru.hse.tftp.packet.DataPacket
import ru.hse.tftp.packet.ErrorPacket
import ru.hse.tftp.packet.ErrorRequest
import ru.hse.tftp.packet.ErrorType
import ru.hse.tftp.packet.Packet
import ru.hse.tftp.packet.parse
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.system.exitProcess

fun sendBytes(inputStream: InputStream, remoteAddress: InetSocketAddress, localSocket: DatagramSocket) {
    inputStream.use { input ->
        val bytes = ByteArray(PACKET_SIZE)

        var i = 0
        while (true) {
            val result = input.read(bytes)

            if (result == -1) {
                break
            }

            sendPacket(DataPacket(bytes, i), localSocket, remoteAddress)

            i++
        }
    }
}

fun sendPacket(packet: Packet, localSocket: DatagramSocket, remoteAddress: InetSocketAddress): InetSocketAddress? {
    val sendRoutine = Thread() {
        sendPacketRoutine(packet, localSocket, remoteAddress)
    }

    sendRoutine.start()

    val address = receiveAcknolegmentRoutine(localSocket, remoteAddress, sendRoutine)
    return address
}


fun receiveAcknolegmentRoutine(localSocket: DatagramSocket, remoteAddress: InetSocketAddress, sendRoutine: Thread): InetSocketAddress? {
    val start = System.currentTimeMillis()
    var result: InetSocketAddress? = null

    val buffer = ByteArray(PACKET_SIZE)

    while (true) {
        val now = System.currentTimeMillis()
        if (now - start > RESPONSE_TIMEOUT) {
            break
        }

        val packet = DatagramPacket(buffer, buffer.size)

        try {
            localSocket.receive(packet)
        } catch (e: IOException) {
            break
        }

        val parseResult = parse(packet)

        if (parseResult is Acknolegment) {
            result = InetSocketAddress(packet.address, packet.port)
            break
        } else if (parseResult is ErrorRequest) {
            println("error: ${parseResult.what}")
            exitProcess(0)
        }
    }

    sendRoutine.interrupt()
    sendRoutine.join()
    return result
}

fun sendPacketRoutine(packet: Packet, localSocket: DatagramSocket, remoteAddress: InetSocketAddress) {
    val startTime = System.currentTimeMillis()

    while (!Thread.interrupted()) {
        val lasted = System.currentTimeMillis() - startTime
        if (lasted > RESPONSE_TIMEOUT) {
            return
        }

        sendPacketWithoutAcknolegment(packet, remoteAddress, localSocket)

        try {
            Thread.sleep(RESPONSE_DELAY.toLong())
        } catch (e: InterruptedException) {
            break
        }
    }
}

fun getBytesToOutputStream(
    userAddress: InetSocketAddress,
    localSocket: DatagramSocket,
    outputStream: FileOutputStream
) {
    var currentBlock = 0
    while (true) {
        val buffer = ByteArray(PACKET_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)

        try {
            localSocket.receive(packet)
        } catch (e: IOException) {
            return
        }

        when (val result = parse(packet)) {
            is Data -> {
                outputStream.write(result.data)

                val acknolegmentPacket = AcknolegmentPacket()
                sendPacket(acknolegmentPacket, localSocket, InetSocketAddress(packet.address, packet.port))
            }

            else -> {
                println("Wrong / Error message from server")
                exitProcess(0)
            }
        }

        currentBlock++
    }
}

fun sendPacketWithoutAcknolegment(packet: Packet, remoteAddress: SocketAddress, localSocket: DatagramSocket) {
    val bytes = packet.byteArray
    localSocket.send(DatagramPacket(bytes, 0, bytes.size, remoteAddress))
}

fun sendErrorPacket(type: ErrorType, remoteAddress: SocketAddress, localSocket: DatagramSocket) {
    sendPacketWithoutAcknolegment(ErrorPacket(type), remoteAddress, localSocket)
}