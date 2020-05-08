package ru.spbau.smirnov.tftp

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.net.io.FromNetASCIIOutputStream
import org.apache.commons.net.io.ToNetASCIIInputStream
import java.io.*
import java.lang.Integer.min

sealed class ServerLogic(protected val connection: Connection) {
    abstract fun handleMessage(message: Message)
    abstract fun start()
    open fun close() {}
}

class BeforeStartLogic(connection: Connection) : ServerLogic(connection) {
    override fun handleMessage(message: Message) {
        when (message) {
            is Acknowledgment -> {
                throw UnexpectedMessage("Acknowledgment received, but no RRQ operation in progress")
            }
            is ReadRequest -> {
                connection.toNewState(ReadRequestLogic(connection, message.filename, message.mode))
            }
            is WriteRequest -> {
                connection.toNewState(WriteRequestLogic(connection, message.filename, message.mode))
            }
            is Data -> {
                throw UnexpectedMessage("Data received, but no WRQ operation in progress")
            }
            is Error -> {
                throw ErrorMessage(message)
            }
            is BrokenMessage -> {
                throw NotTFTPMessage(message)
            }
        }
    }

    override fun start() {}
}

class ReadRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode,
    private val blockSize: Int = 512
) : ServerLogic(connection) {

    private lateinit var sendArray: ByteArray
    private var lastSendBlock = 0

    override fun handleMessage(message: Message) {
        when (message) {
            is Acknowledgment -> {
                receiveAcknowledgment(message)
            }
            is ReadRequest -> {
                if (lastSendBlock == 1 && filename == message.filename && mode == message.mode) {
                    // just ignore, because we will retry sending first block
                } else {
                    throw UnexpectedMessage("RRQ in progress, but other RRQ was requested")
                }
            }
            is WriteRequest -> {
                throw UnexpectedMessage("RRQ is in progress, but WRQ was received")
            }
            is Data -> {
                throw UnexpectedMessage("Data received, but RRQ in progress")
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
            Data(lastSendBlock, sendArray.sliceArray(firstByte until lastByte))
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

class WriteRequestLogic(
    connection: Connection,
    private val filename: String,
    private val mode: TFTPMode,
    private val blockSize: Int = 512
) : ServerLogic(connection) {

    private val timeWaitAfterDone = 5000L
    private var lastReceivedBlock = 0
    private var outputStream: OutputStream? = null

    override fun handleMessage(message: Message) {
        when (message) {
            is Acknowledgment -> {
                throw UnexpectedMessage("ACK received, but WRQ in progress")
            }
            is ReadRequest -> {
                throw UnexpectedMessage("WRQ is in progress, but RRQ was received")
            }
            is WriteRequest -> {
                if (lastReceivedBlock == 0 && message.filename == filename && message.mode == mode) {
                    connection.sendMessageWithoutAcknowledgment(Acknowledgment(0))
                } else {
                    throw UnexpectedMessage("WRQ is in progress, but WRQ was received")
                }
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
                    // Wait some time if acknowledgment was lost
                    GlobalScope.launch {
                        delay(timeWaitAfterDone)
                        connection.close()
                    }
                }
            }
            connection.sendMessageWithoutAcknowledgment(Acknowledgment(message.block))
        } catch (e: IOException) {
            println("Error writing to $filename\n${e.message}")
            throw FileWriteError("Some error while writing to $filename")
        }
    }

    override fun start() {
        println("Start WRQ $filename")
        val file = File("${connection.rootPath}$filename")
        try {
            if (!file.createNewFile()) {
                throw FileAlreadyExists("File $file already exists")
            }
        } catch (e: SecurityException) {
            throw WriteAccessDenied("Have not enough rights for creating file")
        }
        if (!file.canWrite()) {
            throw WriteAccessDenied("$file is not writable")
        }
        when (mode) {
            TFTPMode.OCTET -> outputStream = file.outputStream()
            TFTPMode.NETASCII -> FromNetASCIIOutputStream(file.outputStream())
        }
        connection.sendMessageWithoutAcknowledgment(Acknowledgment(0))
    }

    override fun close() {
        outputStream?.close()
    }
}
