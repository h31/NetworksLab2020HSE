package ru.spb.hse.isomethane.tftp.client

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class TFTPException(message: String) : Exception(message)

class Client {
    private val socket = DatagramSocket()
    var mode = "octet"
    var host: InetAddress = InetAddress.getLocalHost()
    var port = 69

    init {
        socket.soTimeout = Constants.RETRANSMISSION_TIMEOUT
    }

    fun connect(host: InetAddress, port: Int? = null) {
        this.host = host
        this.port = port ?: 69
    }

    fun get(fromFile: String, toFile: String? = null) {
        val output = ByteArrayOutputStream()

        val request = writeTFTP(ReadRequest(fromFile, mode))
        val requestPacket = DatagramPacket(request, request.size, host, port)
        socket.send(requestPacket)

        var block = 1
        var lastPacket = requestPacket
        var retries = 0
        val byteArray = ByteArray(Constants.PACKET_SIZE)

        while (true) {
            val packet = DatagramPacket(byteArray, Constants.PACKET_SIZE)
            try {
                socket.receive(packet)
            } catch (e: SocketTimeoutException) {
                if (retries >= Constants.MAX_RETRIES) {
                    throw TFTPException("Transfer timed out")
                }
                retries++
                socket.send(lastPacket)
                continue
            }
            when (val response = readTFTP(packet.data, packet.length)) {
                is Data -> {
                    if (response.block == block) {
                        output.write(response.data)
                        val ack = writeTFTP(Acknowledgement(block++))
                        lastPacket = DatagramPacket(ack, ack.size, packet.socketAddress)
                        socket.send(lastPacket)
                        if (response.data.size < Constants.DATA_SIZE) {
                            writeFile(toFile ?: fromFile, output.toByteArray())
                            return
                        }
                    }
                }
                is ErrorMessage -> {
                    throw TFTPException(response.errorMessage)
                }
            }
        }
    }

    fun put(fromFile: String, toFile: String? = null) {
        val input = initInput(fromFile)

        val request = writeTFTP(WriteRequest(toFile ?: fromFile, mode))
        val requestPacket = DatagramPacket(request, request.size, host, port)
        socket.send(requestPacket)

        var block = 0
        var lastPacket = requestPacket
        var retries = 0
        val byteArray = ByteArray(Constants.PACKET_SIZE)
        var lastBlock = false

        while (true) {
            val packet = DatagramPacket(byteArray, Constants.PACKET_SIZE)
            try {
                socket.receive(packet)
            } catch (e: SocketTimeoutException) {
                if (retries >= Constants.MAX_RETRIES) {
                    throw TFTPException("Transfer timed out")
                }
                retries++
                socket.send(lastPacket)
                continue
            }
            when (val response = readTFTP(packet.data, packet.length)) {
                is Acknowledgement -> {
                    if (response.block == block) {
                        if (lastBlock) {
                            return
                        }
                        val blockData = input.readNBytes(Constants.DATA_SIZE)
                        if (blockData.size < Constants.DATA_SIZE) {
                            lastBlock = true
                        }
                        val data = writeTFTP(Data(++block, blockData))
                        lastPacket = DatagramPacket(data, data.size, packet.socketAddress)
                        socket.send(lastPacket)
                    }
                }
                is ErrorMessage -> {
                    throw TFTPException(response.errorMessage)
                }
            }
        }
    }

    private fun writeFile(fileName: String, data: ByteArray) {
        val file = File(fileName)
        try {
            file.createNewFile()
        } catch (e: Exception) {
            throw TFTPException("Failed to save result")
        }
        if (mode == "netascii") {
            val fileOutput = file.outputStream()
            val writer = fileOutput.writer(Charsets.US_ASCII)
            val lines = String(data, Charsets.US_ASCII).lines()
            lines.forEachIndexed { index, line ->
                writer.append(line)
                if (index != lines.size - 1) {
                    writer.appendln()
                }
            }
            writer.close()
        } else {
            file.writeBytes(data)
        }
    }

    private fun initInput(fileName: String): ByteArrayInputStream {
        val file = File(fileName)
        if (!file.canRead()) {
            throw TFTPException("Access denied")
        }
        if (file.length() > Constants.MAX_FILE_SIZE) {
            throw TFTPException("File is too large")
        }
        return if (mode == "netascii") {
            val content = ByteArrayOutputStream()
            val writer = content.writer(Charsets.US_ASCII)
            val lines = String(file.readBytes(), Charsets.US_ASCII).lines()
            lines.forEachIndexed { index, line ->
                writer.append(line)
                if (index != lines.size - 1) {
                    writer.append("\r\n")
                }
            }
            writer.close()
            ByteArrayInputStream(content.toByteArray())
        } else {
            ByteArrayInputStream(file.readBytes())
        }
    }
}