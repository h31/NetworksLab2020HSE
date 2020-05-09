package ru.spbau.smirnov.tftp.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.random.Random

class Connection(
    private var inetAddress: InetAddress,
    private var port: Int,
    private val firstMessage: Message,
    val rootPath: String
) {

    private val socket = DatagramSocket(Random.nextInt(1024, 65536))
    private var gotAddress = false
    private val timeout = 10L
    private val timesToSend = 200
    private val bufferSize = 516
    private lateinit var logic: ClientLogic
    /** True if we received all information that we need (but may be other side didn't receive ACK) */
    private var isCompleted = false
    @Volatile private var isFinished = false
    private var messageRoutine: SendMessageRoutine? = null

    fun toNewLogic(newLogic: ClientLogic) {
        logic = newLogic
        newLogic.start()
    }

    fun run() {
        val buffer = ByteArray(bufferSize)
        try {
            when (firstMessage) {
                is ReadRequest -> {
                    logic = BeforeReadRequestLogic(
                        this,
                        firstMessage.filename,
                        firstMessage.mode
                    )
                }
                is WriteRequest -> {
                    logic = BeforeWriteRequestLogic(
                        this,
                        firstMessage.filename,
                        firstMessage.mode
                    )
                }
                else -> throw IllegalArgumentException("First message is not RRQ nor WRQ")
            }
            sendMessageWithAcknowledgment(firstMessage)
            while (!isFinished) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                if (gotAddress && (packet.address != inetAddress || packet.port != port)) {
                    sendError(
                        Error(
                            ErrorCode.UNKNOWN_TRANSFER_ID,
                            "Not a server"
                        ),
                        false,
                        packet.address,
                        packet.port
                    )
                }

                val message = PacketParser.parsePacket(packet)
                handleMessage(message, packet.address, packet.port)
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
                    ErrorCode.NOT_DEFINED,
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
        } catch (e: IOException) {
            println("Something wrong with file or directory: ${e.message}")
        } finally {
            println("Finish $inetAddress $port ${if (isCompleted) "successfully" else "unsuccessfully"}")
            close()
            logic.close()
        }
    }

    fun setAddress(address: InetAddress, port: Int) {
        inetAddress = address
        this.port = port
        gotAddress = true
    }

    private fun handleMessage(message: Message, address: InetAddress, port: Int) {
        if (isFinished) {
            return
        }
        logic.handleMessage(message, address, port)
    }

    private fun sendError(message: Error) {
        sendError(message, false, inetAddress, port)
    }

    private fun sendError(error: Error, blocking: Boolean = true, address: InetAddress, sendPort: Int) {
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

    fun sendMessageWithAcknowledgment(message: Message) {
        val byteArrayMessage = message.toByteArray()
        val packet = DatagramPacket(byteArrayMessage, byteArrayMessage.size, inetAddress, port)
        val routine = SendMessageRoutine(packet, timeout, timesToSend, socket)
        messageRoutine = routine
        GlobalScope.launch {
            routine.start()
        }
    }

    fun sendMessageWithoutAcknowledgment(message: Message) {
        sendMessageWithoutAcknowledgment(message, inetAddress, port)
    }

    private fun sendMessageWithoutAcknowledgment(message: Message, address: InetAddress, port: Int) {
        val byteArrayMessage = message.toByteArray()
        val packet = DatagramPacket(byteArrayMessage, byteArrayMessage.size, address, port)
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
                    // ignored
                }
                delay(timeout)
            }
            if (!isSend) {
                close()
            }
        }

        fun stop() {
            isSend = true
        }
    }
}
