package ru.spbau.smirnov.tftp.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.net.io.FromNetASCIIOutputStream
import org.apache.commons.net.io.ToNetASCIIInputStream
import java.io.*
import java.lang.Integer.min
import java.net.InetAddress

sealed class ClientLogic(protected val connection: Connection) {
    abstract fun handleMessage(message: Message, inetAddress: InetAddress, port: Int)
    abstract fun start()
    open fun close() {}
}

class BeforeWriteRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode
) : ClientLogic(connection) {
    override fun handleMessage(message: Message, inetAddress: InetAddress, port: Int) {
        when (message) {
            is Acknowledgment -> {
                if (message.block != 0) {
                    throw UnexpectedMessage("First ACK message has not zero block")
                }
                connection.stopCurrentSendRoutine()
                connection.setAddress(inetAddress, port)
                connection.toNewLogic(
                    WriteRequestLogic(
                        connection,
                        filename,
                        mode
                    )
                )
            }
            is ReadRequest -> throw IllegalTFTPOperation("Client received RRQ message")
            is WriteRequest -> throw IllegalTFTPOperation("Client received WRQ message")
            is Data -> throw IllegalTFTPOperation("Executing write request, but received data message")
            is Error -> throw ErrorMessage(message)
            is BrokenMessage -> throw NotTFTPMessage(message)
        }
    }

    override fun start() {
    }
}

class BeforeReadRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode
) : ClientLogic(connection) {
    override fun handleMessage(message: Message, inetAddress: InetAddress, port: Int) {
        when (message) {
            is Acknowledgment -> TODO()
            is ReadRequest -> throw IllegalTFTPOperation("Client received RRQ message")
            is WriteRequest -> throw IllegalTFTPOperation("Client received WRQ message")
            is Data -> {
                if (message.block != 1) {
                    throw UnexpectedMessage("First data message has not first block")
                }
                connection.stopCurrentSendRoutine()
                connection.setAddress(inetAddress, port)
                connection.toNewLogic(
                    ReadRequestLogic(
                        connection,
                        filename,
                        mode,
                        message
                    )
                )
            }
            is Error -> throw ErrorMessage(message)
            is BrokenMessage -> throw NotTFTPMessage(message)
        }
    }

    override fun start() {
    }
}

class WriteRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode,
    private val blockSize: Int = 512
) : ClientLogic(connection) {

    private lateinit var sendArray: ByteArray
    private var lastSendBlock = 0

    override fun handleMessage(message: Message, inetAddress: InetAddress, port: Int) {
        when (message) {
            is Acknowledgment -> {
                receiveAcknowledgment(message)
            }
            is ReadRequest -> {
                throw UnexpectedMessage("Client received RRQ")
            }
            is WriteRequest -> {
                throw UnexpectedMessage("Client received WRQ")
            }
            is Data -> {
                throw UnexpectedMessage("Data received, but WRQ in progress")
            }
            is Error -> {
                throw ErrorMessage(message)
            }
            is BrokenMessage -> {
                throw NotTFTPMessage(message)
            }
        }
    }

    override fun start() {
        println("Start RRQ $filename")
        val file = File("${connection.rootPath}$filename")
        if (!file.exists()) {
            throw FileNotFound("File $file not found")
        }
        if (!file.isFile) {
            throw FileNotFound("$file is directory")
        }
        if (!file.canRead()) {
            throw ReadAccessDenied("$file is not readable")
        }
        when (mode) {
            TFTPMode.OCTET -> readBinaryFile(file)
            TFTPMode.NETASCII -> readASCIIFile(file)
        }
        receiveAcknowledgment(Acknowledgment(0))
    }

    private fun receiveAcknowledgment(message: Acknowledgment) {
        if (lastSendBlock == message.block) {
            connection.stopCurrentSendRoutine()
            val leftToSend = lastSendBlock * blockSize
            if (leftToSend > sendArray.size) {
                connection.markCompleted()
                println("RRQ $filename finished")
                connection.close()
                return
            }
            val rightToSend = min(leftToSend + blockSize, sendArray.size)
            lastSendBlock++
            sendBlock(leftToSend, rightToSend)
        }
    }

    private fun sendBlock(firstByte: Int, lastByte: Int) {
        connection.sendMessageWithAcknowledgment(
            Data(
                lastSendBlock,
                sendArray.sliceArray(firstByte until lastByte)
            )
        )
    }

    private fun readBinaryFile(file: File) {
        try {
            sendArray = file.readBytes()
        } catch (e: Throwable) {
            throw FileReadError("Unknown error occurred while reading octet from $file")
        }
    }

    private fun readASCIIFile(file: File) {
        try {
            ToNetASCIIInputStream(ByteArrayInputStream(file.readBytes())).use {
                sendArray = it.readAllBytes()
            }
        } catch (e: Throwable) {
            throw FileReadError("Unknown error occurred while reading ASCII from $file")
        }
    }
}

class ReadRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode,
    private val firstMessage: Data,
    private val blockSize: Int = 512
) : ClientLogic(connection) {

    private val timeWaitAfterDone = 5000L
    private var lastReceivedBlock = 0
    private var outputStream: OutputStream? = null

    override fun handleMessage(message: Message, inetAddress: InetAddress, port: Int) {
        when (message) {
            is Acknowledgment -> {
                throw UnexpectedMessage("ACK received, but WRQ in progress")
            }
            is ReadRequest -> {
                throw UnexpectedMessage("Client received RRQ")
            }
            is WriteRequest -> {
                throw UnexpectedMessage("Client received WRQ")
            }
            is Data -> {
                receiveData(message)
            }
            is Error -> {
                throw ErrorMessage(message)
            }
            is BrokenMessage -> {
                throw NotTFTPMessage(message)
            }
        }
    }

    private fun receiveData(message: Data) {
        try {
            if (message.block == lastReceivedBlock + 1) {
                ++lastReceivedBlock
                outputStream!!.write(message.data)
                outputStream!!.flush()
                if (message.data.size < blockSize) {
                    connection.markCompleted()
                    // Wait some time if acknowledgment was lost
                    GlobalScope.launch {
                        delay(timeWaitAfterDone)
                        connection.close()
                    }
                }
            }
            connection.sendMessageWithoutAcknowledgment(
                Acknowledgment(
                    message.block
                )
            )
        } catch (e: IOException) {
            println("Error writing to $filename\n${e.message}")
            throw FileWriteError("Some error while writing to $filename")
        }
    }

    override fun start() {
        println("Start WRQ $filename")
        val file = File("${connection.rootPath}$filename")
        println("${connection.rootPath}${file.name}")
        try {
            file.createNewFile()
        } catch (e: SecurityException) {
            throw WriteAccessDenied("Have not enough rights for creating file")
        }
        if (!file.canWrite()) {
            throw WriteAccessDenied("$file is not writable")
        }
        outputStream = when (mode) {
            TFTPMode.OCTET -> file.outputStream()
            TFTPMode.NETASCII -> FromNetASCIIOutputStream(file.outputStream())
        }
        receiveData(firstMessage)
    }

    override fun close() {
        outputStream?.close()
    }
}
