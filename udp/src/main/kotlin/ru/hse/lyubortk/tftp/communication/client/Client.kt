package ru.hse.lyubortk.tftp.communication.client

import ru.hse.lyubortk.tftp.TFTP_DATA_MAX_LENGTH
import ru.hse.lyubortk.tftp.TFTP_SERVER_PORT
import ru.hse.lyubortk.tftp.communication.BaseCommunicator
import ru.hse.lyubortk.tftp.communication.withConversions
import ru.hse.lyubortk.tftp.model.*
import java.io.File
import java.io.OutputStream
import java.net.InetSocketAddress

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

    val mode: Mode = when (modeString.toLowerCase()) {
        "netascii" -> Mode.NETASCII
        "octet" -> Mode.OCTET
        else -> {
            println("Cannot recognize specified mode. Will use OCTET")
            Mode.OCTET
        }
    }

    println("method: $method, file: $fileName, ip: $serverIp, port: $serverPort, mode: $mode")

    val serverAddress = InetSocketAddress(serverIp, serverPort)
    Client(serverAddress).use {
        when (method) {
            "PUT" -> it.put(fileName, mode)
            "GET" -> it.get(fileName, mode)
            else -> printUsageAndThrowError()
        }
    }
}

@kotlin.ExperimentalUnsignedTypes
class Client(private val serverAddress: InetSocketAddress) : BaseCommunicator() {
    fun put(fileName: String, mode: Mode) {
        val inputStream = File(fileName).inputStream().withConversions(mode)

        val acknowledgmentAndAddress = sendMessageWithRetry(WriteRequest(fileName, mode), serverAddress, true) {
            (it as? Acknowledgment)?.takeIf { acknowledgment ->
                acknowledgment.blockNumber == 0.toUShort()
            }
        }
        if (acknowledgmentAndAddress == null) {
            System.err.println(NO_ACKNOWLEDGMENT_MESSAGE)
            return
        }
        sendData(inputStream, acknowledgmentAndAddress.second)
    }

    fun get(fileName: String, mode: Mode) {
        val outputStream = File(fileName).outputStream().withConversions(mode)
        receiveData(outputStream, ReadRequest(fileName, mode))
    }

    private fun receiveData(outputStream: OutputStream, readRequest: ReadRequest) {
        outputStream.use { output ->
            var blockNumber: UShort = 1u

            fun validateDataMessage(message: Message): Data? =
                (message as? Data)?.takeIf { data -> data.blockNumber == blockNumber }

            val dataAndNewAddress = sendMessageWithRetry(readRequest, serverAddress, true) {
                validateDataMessage(it)
            }
            if (dataAndNewAddress == null) {
                // Should not send error message to main server port
                System.err.println(NO_DATA_MESSAGE)
                return
            }
            val (firstDataMessage, remoteAddress) = dataAndNewAddress
            output.write(firstDataMessage.data)

            blockNumber++ // Overflow is totally fine!
            var lastDataSize = firstDataMessage.data.size
            while (lastDataSize == TFTP_DATA_MAX_LENGTH) {
                val data = sendMessageWithRetry(Acknowledgment((blockNumber - 1u).toUShort()), remoteAddress) {
                    validateDataMessage(it)
                }?.first
                if (data == null) {
                    sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_DATA_MESSAGE), remoteAddress)
                    System.err.println(NO_DATA_MESSAGE)
                    return
                }
                lastDataSize = data.data.size
                output.write(data.data)
                blockNumber++ // Overflow is totally fine!
            }
            sendMessage(Acknowledgment((blockNumber - 1u).toUShort()), remoteAddress)
        }
    }
}