package ru.spbau.smirnov.tftp.server

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.random.Random

class Connection(
    private val inetAddress: InetAddress,
    private val port: Int,
    private val server: Server,
    private val firstMessage: Message
) : Thread() {

    private val socket = DatagramSocket(Random.nextInt(1024, 65536))
    val rootPath = server.rootPath
    private val timeout = 10L
    private val timesToSend = 200
    private val bufferSize = 516
    /** True if we received all information that we need (but may be other side didn't receive ACK) */
    private var isCompleted = false
    @Volatile private var isFinished = false
    private var messageRoutine: SendMessageRoutine? = null
    private var logic: ServerLogic = BeforeStartLogic(this)

    override fun run() {
        var lastMessage = firstMessage
        val buffer = ByteArray(bufferSize)
        try {
            while (!isFinished) {

                handleMessage(lastMessage)

                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                if (packet.address != inetAddress || packet.port != port) {
                    sendError(
                        Error(
                            ErrorCode.UNKNOWN_TRANSFER_ID,
                            "Wrong address or port"
                        ), false, packet.address, packet.port)
                    continue
                }

                lastMessage = PacketParser.parsePacket(packet)
            }
        } catch (e: NotTFTPMessage) {
            sendError(
                Error(
                    ErrorCode.ILLEGAL_OPERATION,
                    e.brokenMessage.error
                )
            )
        } catch (e: FileNotFound) {
            sendError(
                Error(
                    ErrorCode.FILE_NOT_FOUND,
                    e.message!!
                )
            )
        } catch (e: FileAlreadyExists) {
            sendError(
                Error(
                    ErrorCode.FILE_EXISTS,
                    e.message!!
                )
            )
        } catch (e: ErrorMessage) {
            println("Received error message. Code: ${e.errorMessage.errorCode} message: ${e.errorMessage.errorMessage}")
        } catch (e: ReadAccessDenied) {
            sendError(
                Error(
                    ErrorCode.ACCESS_VIOLATION,
                    e.message!!
                )
            )
        } catch (e: FileReadError) {
            sendError(
                Error(
                    ErrorCode.NOT_DEFINED,
                    e.message!!
                )
            )
        } catch (e: SocketException) {
            if (!isCompleted) {
                println("Connection broken\n${e.message}")
            }
        } catch (e: UnexpectedMessage) {
            sendError(
                Error(
                    ErrorCode.ILLEGAL_OPERATION,
                    e.message!!
                )
            )
        } finally {
            println("Finish $inetAddress $port ${if (isCompleted) "Successfully" else "Unsuccessfully"}")
            close()
            logic.close()
            server.finishConnection(inetAddress, port)
        }
    }

    fun toNewState(newLogic: ServerLogic) {
        logic = newLogic
        logic.start()
    }

    private fun handleMessage(message: Message) {
        if (isFinished) {
            return
        }
        logic.handleMessage(message)
    }

    private fun sendError(
        error: Error,
        blocking: Boolean = true,
        address: InetAddress = inetAddress,
        sendPort: Int = port
    ) {
        if (blocking) {
            runBlocking {
                sendMessageWithoutAcknowledgment(error, address, sendPort)
            }
        } else {
            GlobalScope.launch {
                sendMessageWithoutAcknowledgment(error, address, sendPort)
            }
        }
    }

    fun sendMessageWithAcknowledgment(
        message: Message,
        address: InetAddress = inetAddress,
        sendPort: Int = port
    ) {
        val byteArrayMessage = message.toByteArray()
        val packet = DatagramPacket(byteArrayMessage, byteArrayMessage.size, address, sendPort)
        val routine = SendMessageRoutine(packet, timeout, timesToSend, socket)
        messageRoutine = routine
        GlobalScope.launch {
            routine.start()
        }
    }

    fun sendMessageWithoutAcknowledgment(
        message: Message,
        address: InetAddress = inetAddress,
        sendPort: Int = port
    ) {
        val byteArrayMessage = message.toByteArray()
        val packet = DatagramPacket(byteArrayMessage, byteArrayMessage.size, address, sendPort)
        try {
            socket.send(packet)
        } catch (e: SocketException) {
            println("Error while sending\n${e.message}")
        }
    }

    fun stopCurrentSendRoutine() {
        messageRoutine?.stop()
        messageRoutine = null
    }

    fun markCompleted() {
        isCompleted = true
    }

    fun close() {
        isFinished = true
        socket.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private inner class SendMessageRoutine(
        private val packet: DatagramPacket,
        private val timeout: Long,
        private val timesToSend: Int,
        private val socket: DatagramSocket
    ) {
        @Volatile private var isSend = false

        suspend fun start() {
            for (currentTry in 1..timesToSend) {
                if (isSend) {
                    break
                }
                try {
                    socket.send(packet)
                } catch (e: SocketException) {
                    println("Socket exception while trying to send\n${e.message}")
                }
                delay(timeout)
            }
            if (!isSend) {
                println("Have no accept. Closing connection")
                close()
            }
        }

        fun stop() {
            isSend = true
        }
    }
}
