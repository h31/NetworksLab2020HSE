package ru.hse.tftp.packet

import ru.hse.tftp.packet.ErrorType.ACCESS_VIOLATION
import ru.hse.tftp.packet.ErrorType.DISK_FULL_OR_ALLOCATION_EXCEEDED
import ru.hse.tftp.packet.ErrorType.FILE_ALREADY_EXISTS
import ru.hse.tftp.packet.ErrorType.FILE_NOT_FOUND
import ru.hse.tftp.packet.ErrorType.ILLEGAL_TFTP_OPERATION
import ru.hse.tftp.packet.ErrorType.NO_SUCH_USER
import ru.hse.tftp.packet.ErrorType.PARSE_ERROR
import ru.hse.tftp.packet.ErrorType.UNKNOWN_TRANSFER_ID
import ru.hse.tftp.packet.MessageType.ACKNOWLEDGEMENT
import ru.hse.tftp.packet.MessageType.DATA
import ru.hse.tftp.packet.MessageType.ERROR
import ru.hse.tftp.packet.MessageType.READ_REQUEST
import ru.hse.tftp.packet.MessageType.WRITE_REQUEST
import ru.hse.tftp.packet.UserRequest.Mode
import ru.hse.tftp.packet.UserRequest.Mode.OCTET
import java.io.ByteArrayOutputStream

enum class MessageType(val value: Int) {
    READ_REQUEST(1),
    WRITE_REQUEST(2),
    DATA(3),
    ACKNOWLEDGEMENT(4),
    ERROR(5),
}

sealed class Packet(val messageType: MessageType) {
    abstract val byteArray: ByteArray
}

private fun ByteArrayOutputStream.writeCode(code: Int) {
    write(code shr 8)
    write(code and 0xff)
}

class ReadPacket(private val filename: String, private val mode: Mode = OCTET): Packet(READ_REQUEST) {
    override val byteArray: ByteArray
        get() {
            val outputStream = ByteArrayOutputStream()
            outputStream.writeCode(messageType.value)
            outputStream.write(filename.toByteArray())
            return outputStream.toByteArray()
        }
}

class WritePacket(private val filename: String, private val mode: Mode = OCTET): Packet(WRITE_REQUEST) {
    override val byteArray: ByteArray
        get() {
            val outputStream = ByteArrayOutputStream()
            outputStream.writeCode(messageType.value)
            outputStream.write(filename.toByteArray())
            return outputStream.toByteArray()
        }
}

class DataPacket(private val data: ByteArray, private val blockNumber: Int): Packet(DATA) {
    override val byteArray: ByteArray
        get() {
            val outputStream = ByteArrayOutputStream()
            outputStream.writeCode(messageType.value)
            outputStream.writeCode(blockNumber)
            outputStream.write(data)
            return outputStream.toByteArray()
        }
}

class AcknolegmentPacket(): Packet(ACKNOWLEDGEMENT) {
    override val byteArray: ByteArray
        get() {
            val outputStream = ByteArrayOutputStream()
            outputStream.writeCode(messageType.value)
            return outputStream.toByteArray()
        }
}

class ErrorPacket(private val type: ErrorType) : Packet(ERROR) {
    val message = when (type) {
        PARSE_ERROR -> "Error parsing request"
        FILE_NOT_FOUND -> "File not found on server"
        ACCESS_VIOLATION -> "Access violation"
        DISK_FULL_OR_ALLOCATION_EXCEEDED -> "Dis is full or allocation exceeded"
        ILLEGAL_TFTP_OPERATION -> "Illegal TFTP operation"
        UNKNOWN_TRANSFER_ID -> "unknown  transfer id"
        FILE_ALREADY_EXISTS -> "File already exists on server"
        NO_SUCH_USER -> "No such user on server"
    }

    override val byteArray: ByteArray
        get() {
            val outputStream = ByteArrayOutputStream()
            outputStream.writeCode(messageType.value)
            outputStream.writeCode(type.code)
            outputStream.write(message.toByteArray())
            return outputStream.toByteArray()
        }
}