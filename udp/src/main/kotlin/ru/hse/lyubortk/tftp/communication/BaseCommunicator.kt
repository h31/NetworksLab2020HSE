package ru.hse.lyubortk.tftp.communication

import ru.hse.lyubortk.tftp.TFTP_DATA_MAX_LENGTH
import ru.hse.lyubortk.tftp.TFTP_PACKET_MAX_LENGTH
import ru.hse.lyubortk.tftp.model.*
import java.io.Closeable
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

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

    protected fun sendData(inputStream: InputStream, remoteAddress: InetSocketAddress) {
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
        remoteAddress: InetSocketAddress,
        acceptOtherRemotePort: Boolean = false, // needed for client (server switches its port)
        responseValidation: (Message) -> T?
    ): Pair<T, InetSocketAddress>? {
        val startTime = System.currentTimeMillis()
        fun getRemainingTime() = max(0, startTime + RESPONSE_TIMEOUT_MILLIS - System.currentTimeMillis())

        var lastRetryTime = 0L
        fun getTimeToNextRetry() = max(0, lastRetryTime + RETRY_TIME_INTERVAL - System.currentTimeMillis())

        while (getRemainingTime() > 0) {
            if (getTimeToNextRetry() == 0L) { // Do not spam remote socket!! (Spam can occur in case of multiple old messages)
                lastRetryTime = System.currentTimeMillis()
                sendMessage(message, remoteAddress)
            }

            val futures = singleThreadExecutor.invokeAll(mutableListOf(Callable {
                val receivedPacket = DatagramPacket(ByteArray(TFTP_PACKET_MAX_LENGTH), TFTP_PACKET_MAX_LENGTH)
                socket.receive(receivedPacket)
                receivedPacket
            }), min(getTimeToNextRetry(), getRemainingTime()), TimeUnit.MILLISECONDS)

            if (futures[0].isCancelled) {
                continue
            }
            val packet = futures[0].get()
            if (packet.address != remoteAddress.address ||
                (packet.port != remoteAddress.port && !acceptOtherRemotePort) // this message is not from our client
            ) {
                // Strange, but RFC says we should do this
                sendMessage(ErrorMessage(ErrorType.UNKNOWN_TRANSFER_ID, UNKNOWN_TID_MESSAGE), packet.socketAddress)
                continue
            }
            val receivedMessage = try {
                Deserializer.deserialize(packet.data.copyOf(packet.length))
            } catch (e: DeserializationException) { // client sends incorrect messages. do not retry
                System.err.println("Cannot deserialize message: $e")
                return null
            }
            val validatedMessage = responseValidation(receivedMessage)
            if (validatedMessage != null) {
                return validatedMessage to InetSocketAddress(packet.address, packet.port) // OK!
            }
            printUnexpectedMessage(receivedMessage, packet.socketAddress)
            if (receivedMessage is ErrorMessage) {
                //RFC states that error packet from correct remote address terminate the connection
                return null
            }
            // probably old message
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
        private const val RETRY_TIME_INTERVAL = 4500L
        private const val UNKNOWN_TID_MESSAGE = "Unknown transfer ID"
    }
}