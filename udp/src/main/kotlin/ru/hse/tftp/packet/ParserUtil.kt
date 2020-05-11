package ru.hse.tftp.packet

import ru.hse.tftp.packet.ErrorType.PARSE_ERROR
import ru.hse.tftp.packet.UserRequest.Mode.OCTET
import java.net.DatagramPacket

fun parse(packet: DatagramPacket): ParseResult {
    return parse(packet.data, packet.offset, packet.length)
}

fun parse(data: ByteArray, offset: Int, length: Int): ParseResult {
    val code = getInt(data, offset)

    val result: ParseResult =
    when (code) {
        1 -> {
            val filename = getString(data, offset + 2, offset + length)
            ReadRequest(filename)
        }

        2 -> {
            val filename = getString(data, offset + 2, offset + length)
            WriteRequest(filename)
        }

        3 -> {
            val blockNumber = getInt(data, offset + 2)

            var end = offset + 4
            while (end < offset + length && data[end] != 0.toByte()) {
                end++
            }

            val receivedData = data.sliceArray(offset+ 4 until end)
            Data(receivedData, blockNumber)
        }

        4 -> {
            Acknolegment()
        }

        5 -> {
            val errorCode = getInt(data, offset + 2)
            ErrorRequest(ErrorType.fromCode(errorCode))
        }

        else -> {
            ErrorRequest(PARSE_ERROR)
        }
    }

    return result
}

fun getString(data: ByteArray, begin: Int, end: Int): String {
    return String(data.sliceArray(begin until end))
}

fun getInt(data: ByteArray, offset: Int): Int {
    return (data[offset].toInt() shl 8) or (data[offset + 1].toInt())
}

open class ParseResult {

}

open class UserRequest(val filename: String, val mode: Mode) : ParseResult() {
    enum class Mode(val string: String) {
        OCTET("octet"), NETASCII("netascii")
    }
}

class ReadRequest(filename: String, mode: Mode = OCTET) : UserRequest(filename, mode) {

}

class WriteRequest(filename: String, mode: Mode = OCTET) : UserRequest(filename, mode) {

}

class Data(val data: ByteArray, val blockNumber: Int) : ParseResult() {

}

class Acknolegment() : ParseResult() {

}

class ErrorRequest(val what: ErrorType) : ParseResult() {
}

enum class ErrorType(val code: Int) {
    PARSE_ERROR(0),
    FILE_NOT_FOUND(1),
    ACCESS_VIOLATION(2),
    DISK_FULL_OR_ALLOCATION_EXCEEDED(3),
    ILLEGAL_TFTP_OPERATION(4),
    UNKNOWN_TRANSFER_ID(5),
    FILE_ALREADY_EXISTS(6),
    NO_SUCH_USER(7);

    companion object {
        fun fromCode(code: Int): ErrorType {
            return values().first { it.code == code }
        }
    }
}