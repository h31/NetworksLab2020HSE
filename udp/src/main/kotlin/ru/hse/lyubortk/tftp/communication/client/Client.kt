package ru.hse.lyubortk.tftp.communication.client

import ru.hse.lyubortk.tftp.TFTP_SERVER_PORT
import ru.hse.lyubortk.tftp.communication.BaseCommunicator
import ru.hse.lyubortk.tftp.communication.withConversions
import ru.hse.lyubortk.tftp.model.*
import java.io.File
import java.net.InetSocketAddress
import java.net.SocketAddress

private fun printUsageAndThrowError(): Nothing {
    System.err.println("Cannot parse arguments")
    println("Usage:")
    println("./gradlew clientGet -Pfile=filename -Pip=serverIp [-Pmode=(OCTET/NETASCII)] [-Pport=serverPort]")
    println("./gradlew clientPut -Pfile=filename -Pip=serverIp [-Pmode=(OCTET/NETASCII)] [-Pport=serverPort]")
    error("cannot parse arguments")
}

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val method = args.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: printUsageAndThrowError()
    val fileName = args.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: printUsageAndThrowError()
    val serverIp = args.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: printUsageAndThrowError()
    val modeString = args.getOrElse(3) { "octet" }
    val serverPort = args.getOrNull(4)?.toIntOrNull() ?: TFTP_SERVER_PORT

    println("method: $method, file: $fileName, ip: $serverIp, port: $serverPort")

    val mode: Mode = when (modeString.toLowerCase()) {
        "netascii" -> Mode.NETASCII
        "octet" -> Mode.OCTET
        else -> {
            println("Cannot recognize specified mode. Will use OCTET")
            Mode.OCTET
        }
    }

    val clientAddress = InetSocketAddress(serverIp, serverPort)
    Client(clientAddress).use {
        when (method) {
            "PUT" -> it.put(fileName, mode)
            "GET" -> it.get(fileName, mode)
            else -> printUsageAndThrowError()
        }
    }
}

@kotlin.ExperimentalUnsignedTypes
class Client(clientAddress: SocketAddress) : BaseCommunicator(clientAddress) {
    fun put(fileName: String, mode: Mode) {
        val inputStream = File(fileName).inputStream().withConversions(mode)

        val acknowledgment = sendMessageWithRetry(WriteRequest(fileName, mode)) {
            (it as? Acknowledgment)?.takeIf { acknowledgment ->
                acknowledgment.blockNumber == 0.toUShort()
            }
        }
        if (acknowledgment == null) {
            sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_ACKNOWLEDGMENT_MESSAGE))
            System.err.println(NO_ACKNOWLEDGMENT_MESSAGE)
        }

        sendData(inputStream)
    }

    fun get(fileName: String, mode: Mode) {
        val outputStream = File(fileName).outputStream().withConversions(mode)
        receiveData(outputStream, ReadRequest(fileName, mode))
    }
}