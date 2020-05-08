package ru.hse.lyubortk.tftp.communication

import ru.hse.lyubortk.tftp.TFTP_DATA_MAX_LENGTH
import ru.hse.lyubortk.tftp.TFTP_PACKET_MAX_LENGTH
import ru.hse.lyubortk.tftp.model.*
import java.io.Closeable
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.math.max

@kotlin.ExperimentalUnsignedTypes
abstract class BaseCommunicator : Closeable {
    private val socket: DatagramSocket = DatagramSocket()
    private val singleThreadExecutor = Executors.newSingleThreadExecutor(object : ThreadFactory {
        val defaultFactory = Executors.defaultThreadFactory()
        override fun newThread(r: Runnable): Thread {
            val thread = defaultFactory.newThread(r)
            thread.isDaemon = true
            return thread
        }
    })

    protected fun sendData(inputStream: InputStream, remoteAddress: SocketAddress) {
        inputStream.use { input ->
            val bytes = ByteArray(TFTP_DATA_MAX_LENGTH)
            var blockNumber: UShort = 1u
            do {
                val readBytes = max(0, input.read(bytes))
                val acknowledgment = sendMessageWithRetry(Data(blockNumber, bytes.copyOf(readBytes)), remoteAddress) {
                    (it as? Acknowledgment)?.takeIf { acknowledgment ->
                        acknowledgment.blockNumber == blockNumber
                    }
                }
                if (acknowledgment == null) {
                    sendMessage(ErrorMessage(ErrorType.NOT_DEFINED, NO_ACKNOWLEDGMENT_MESSAGE), remoteAddress)
                    System.err.println(NO_ACKNOWLEDGMENT_MESSAGE)
                    return
                }
                blockNumber++ // Overflow is totally fine!
            } while (readBytes == 512)
        }
    }

    protected fun <T : Message> sendMessageWithRetry(
        message: Message,
        remoteAddress: SocketAddress,
        responseValidation: (Message) -> T?
    ): Pair<T, SocketAddress>? {
        val startTime = System.currentTimeMillis()
        fun getRemainingTime() = max(0, startTime + RESPONSE_TIMEOUT_MILLIS - System.currentTimeMillis())

        while (getRemainingTime() > 0) {
            sendMessage(message, remoteAddress)

            val futures = singleThreadExecutor.invokeAll(mutableListOf(Callable {
                val receivedPacket = DatagramPacket(ByteArray(TFTP_PACKET_MAX_LENGTH), TFTP_PACKET_MAX_LENGTH)
                socket.receive(receivedPacket)
                receivedPacket
            }), getRemainingTime(), TimeUnit.MILLISECONDS)

            if (futures[0].isCancelled) {
                continue
            }
            val packet = futures[0].get()
            val receivedMessage = try {
                Deserializer.deserialize(packet.data.copyOf(packet.length))
            } catch (e: DeserializationException) {
                System.err.println("Cannot deserialize message: $e")
                continue
            }
            val validatedMessage = responseValidation(receivedMessage)
            if (validatedMessage != null) {
                return validatedMessage to packet.socketAddress
            }
            printUnexpectedMessage(receivedMessage, packet.socketAddress)
        }
        return null
    }

    protected fun sendMessage(message: Message, remoteAddress: SocketAddress) {
        val bytes = Serializer.serialize(message)
        val packet = DatagramPacket(bytes, 0, bytes.size, remoteAddress)
        socket.send(packet)
    }

    private fun printUnexpectedMessage(message: Message, remoteAddress: SocketAddress) {
        when (message) {
            is ErrorMessage -> System.err.println(
                "Received error of type ${message.errorType.name}: ${message.errorMessage} from address $remoteAddress"
            )
            else -> System.err.println(
                "Received unexpected message of type ${message.javaClass.simpleName} from address $remoteAddress"
            )
        }
    }

    override fun close() {
        socket.close()
        singleThreadExecutor.shutdownNow()
    }

    companion object BaseCommunicator {
        internal const val NO_ACKNOWLEDGMENT_MESSAGE = "Did not receive any acknowledgment for sent packet"
        internal const val NO_DATA_MESSAGE = "Did not receive any data"
        private const val RESPONSE_TIMEOUT_MILLIS = 15000L
    }
}