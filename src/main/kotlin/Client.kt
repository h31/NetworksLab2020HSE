package ru.hse.spb

import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    println("Please enter host address:")
    val address = scanner.nextLine().trim()
    println("Please enter host port:")
    val port = scanner.nextLine().trim().toInt()
    val socketAddress = InetSocketAddress(address, port)
    println("Please enter mode(netascii or octet):")
    val mode = scanner.nextLine().trim()
    while (true) {
        when (scanner.nextLine()) {
            "get" -> {
                println("Please enter file name:")
                val fileName = scanner.nextLine().trim()
                val initialPacket =
                    RRQPacket(StandardCharsets.US_ASCII.encode(fileName), StandardCharsets.US_ASCII.encode(mode))
                val socket = DatagramSocket()
                println("Receiving file...")
                sendPacket(initialPacket, socket, socketAddress)
                receiveFile(fileName, mode, socketAddress, socket, false)
                println("Finished...")
            }
            "put" -> {
                println("Please enter file name:")
                val fileName = scanner.nextLine().trim()
                val initialPacket =
                    WRQPacket(StandardCharsets.US_ASCII.encode(fileName), StandardCharsets.US_ASCII.encode(mode))
                val socket = DatagramSocket()
                sendPacket(initialPacket, socket, socketAddress)
                val ackPacket = waitAckPacket(0.toUShort(), socket, 5000)
                if (ackPacket is AckPacket) {
                    println("Sending file...")
                    sendFile(fileName, mode, ackPacket.socketAddress!!, socket)
                    println("Finished")
                } else {
                    println("Failed to send file")
                }
            }
            else -> {
                println("Please enter 'get' or 'put'")
            }
        }
    }
}