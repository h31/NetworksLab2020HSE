package ru.spbau.smirnov.tftp.client

import org.apache.commons.net.io.ToNetASCIIInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

sealed class Message(protected val code: Int) {
    abstract fun toByteArray(): ByteArray
}

class Acknowledgment(val block: Int) : Message(4) {
    override fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTwoByteInteger(outputStream, code)
        writeTwoByteInteger(outputStream, block)
        return outputStream.toByteArray()
    }
}

class ReadRequest(val filename: String, val mode: TFTPMode) : Message(1) {
    override fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTwoByteInteger(outputStream, code)
        writeNetASCIIString(outputStream, filename)
        writeNetASCIIString(outputStream, mode.string)
        return outputStream.toByteArray()
    }
}

class WriteRequest(val filename: String, val mode: TFTPMode) : Message(2) {
    override fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTwoByteInteger(outputStream, code)
        writeNetASCIIString(outputStream, filename)
        writeNetASCIIString(outputStream, mode.string)
        return outputStream.toByteArray()
    }
}

class Data(val block: Int, val data: ByteArray) : Message(3) {
    override fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTwoByteInteger(outputStream, code)
        writeTwoByteInteger(outputStream, block)
        outputStream.writeBytes(data)
        return outputStream.toByteArray()
    }
}

class Error(val errorCode: ErrorCode, val errorMessage: String) : Message(5) {
    override fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writeTwoByteInteger(outputStream, code)
        writeTwoByteInteger(outputStream, errorCode.code)
        writeNetASCIIString(outputStream, errorMessage)
        return outputStream.toByteArray()
    }
}

class BrokenMessage(val error: String) : Message(-1) {
    override fun toByteArray(): ByteArray {
        throw IllegalStateException("Broken message should not be converted to byte array")
    }
}

fun writeTwoByteInteger(outputStream: ByteArrayOutputStream, block: Int) {
    outputStream.write(block shr 8)
    outputStream.write(block and 0xff)
}

fun writeNetASCIIString(outputStream: ByteArrayOutputStream, string: String) {
    ToNetASCIIInputStream(ByteArrayInputStream(string.toByteArray(Charsets.US_ASCII))).use {
        outputStream.writeBytes(it.readAllBytes())
        outputStream.write(0)
    }
}
