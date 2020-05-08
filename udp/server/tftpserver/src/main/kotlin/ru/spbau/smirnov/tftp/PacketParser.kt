package ru.spbau.smirnov.tftp

import org.apache.commons.net.io.FromNetASCIIInputStream
import java.io.ByteArrayInputStream
import java.lang.IllegalArgumentException
import java.net.DatagramPacket

object PacketParser {
    private const val PARSE_REQUEST_CODE = 1
    private const val WRITE_REQUEST_CODE = 2
    private const val DATA_CODE = 3
    private const val ACKNOWLEDGMENT_CODE = 4
    private const val ERROR_CODE = 5

    fun parsePacket(packet: DatagramPacket): Message {
        val data = packet.data.sliceArray(0 until packet.length)

        return try {
            val (operationCode, tail) = readTwoByteInteger(data)

            when (operationCode) {
                PARSE_REQUEST_CODE -> parseReadRequest(tail)
                WRITE_REQUEST_CODE -> parseWriteRequest(tail)
                DATA_CODE -> parseData(tail)
                ACKNOWLEDGMENT_CODE -> parseAcknowledgment(tail)
                ERROR_CODE -> parseError(tail)
                else -> throw IllegalTFTPOperation("Unexpected operation type ${data[0]}${data[1]}")
            }
        } catch (e: IllegalTFTPOperation) {
            BrokenMessage(e.message!!)
        } catch (e: IllegalArgumentException) {
            BrokenMessage("Wrong mode")
        }
    }

    private fun parseReadRequest(request: ByteArray): ReadRequest {
        val (filename, mode) = readRequest(request)
        return ReadRequest(filename, TFTPMode.valueOf(mode.toUpperCase()))
    }

    private fun parseWriteRequest(request: ByteArray): WriteRequest {
        val (filename, mode) = readRequest(request)
        return WriteRequest(filename, TFTPMode.valueOf(mode.toUpperCase()))
    }

    private fun parseData(data: ByteArray): Data {
        val (block, dataTail) = readTwoByteInteger(data)
        return Data(block, dataTail)
    }

    private fun parseAcknowledgment(acknowledgment: ByteArray): Acknowledgment {
        val (block, emptyTail) = readTwoByteInteger(acknowledgment)
        if (emptyTail.isNotEmpty()) {
            throw IllegalTFTPOperation("Unexpected bytes ${emptyTail.size}")
        }
        return Acknowledgment(block)
    }

    private fun parseError(error: ByteArray): Error {
        val (code, errorTail) = readTwoByteInteger(error)
        if (code !in 0..7) {
            throw IllegalTFTPOperation("Unexpected error code")
        }
        val (message, emptyTail) = readString(errorTail)
        if (emptyTail.isNotEmpty()) {
            throw IllegalTFTPOperation("Unexpected bytes")
        }
        return Error(ErrorCode.byCode(code), message)
    }

    private fun readRequest(request: ByteArray): Pair<String, String> {
        val (filename, modeTail) = readString(request)
        if (filename.isEmpty()) {
            throw IllegalTFTPOperation("Filename cannot be empty")
        }
        val (mode, emptyTail) = readString(modeTail)
        if (emptyTail.isNotEmpty()) {
            throw IllegalTFTPOperation("Unexpected bytes")
        }
        return filename to mode
    }

    private fun readString(array: ByteArray): Pair<String, ByteArray> {
        val endOfString = array.indexOf(0)
        if (endOfString == -1) {
            throw IllegalTFTPOperation("String doesn't ends with 0")
        }
        val stringArray = array.sliceArray(0 until endOfString)
        return convertNetASCIIToString(stringArray) to array.sliceArray((endOfString + 1)..array.lastIndex)
    }

    private fun readTwoByteInteger(array: ByteArray): Pair<Int, ByteArray> {
        if (array.size < 2) {
            throw IllegalTFTPOperation("Message is too short")
        }
        val value = (array[0].toInt() and 0xff) * 256 + (array[1].toInt() and 0xff)
        return value to array.sliceArray(2..array.lastIndex)
    }

    private fun convertNetASCIIToString(netASCIIString: ByteArray): String {
        FromNetASCIIInputStream(ByteArrayInputStream(netASCIIString)).use {
            return String(it.readAllBytes(), Charsets.US_ASCII)
        }
    }
}
