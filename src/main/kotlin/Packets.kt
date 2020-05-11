package ru.hse.spb

import java.net.DatagramPacket
import java.net.SocketAddress
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.text.ParseException

open class Packet

data class RRQPacket(val filename: ByteBuffer, val mode: ByteBuffer) : Packet()
data class WRQPacket(val filename: ByteBuffer, val mode: ByteBuffer) : Packet()
data class DataPacket(val blockNumber: UShort, val data: ByteBuffer) : Packet()
data class AckPacket(val blockNumber: UShort, var socketAddress: SocketAddress? = null) : Packet()
data class ErrorPacket(val errorCode: UShort, val errMsg: ByteBuffer) : Packet()

fun toByteBuffer(packet: Packet): ByteBuffer {
    when (packet) {
        is RRQPacket -> {
            val buffer = ByteBuffer.allocate(4 + packet.filename.remaining() + packet.mode.remaining())
            buffer.putShort(1)
            buffer.put(packet.filename)
            buffer.put(0.toByte())
            buffer.put(packet.mode)
            buffer.put(0.toByte())
            return buffer
        }
        is WRQPacket -> {
            val buffer = ByteBuffer.allocate(4 + packet.filename.remaining() + packet.mode.remaining())
            buffer.putShort(2)
            buffer.put(packet.filename)
            buffer.put(0.toByte())
            buffer.put(packet.mode)
            buffer.put(0.toByte())
            return buffer
        }
        is DataPacket -> {
            val buffer = ByteBuffer.allocate(4 + packet.data.remaining())
            buffer.putShort(3)
            buffer.putShort(packet.blockNumber.toShort())
            buffer.put(packet.data)
            return buffer
        }
        is AckPacket -> {
            val buffer = ByteBuffer.allocate(4)
            buffer.putShort(4)
            buffer.putShort(packet.blockNumber.toShort())
            return buffer
        }
        is ErrorPacket -> {
            val buffer = ByteBuffer.allocate(5 + packet.errMsg.remaining())
            buffer.putShort(5)
            buffer.putShort(packet.errorCode.toShort())
            buffer.put(packet.errMsg)
            buffer.put(0.toByte())
            return buffer
        }
        else -> {
            return ByteBuffer.allocate(0)
        }
    }
}

fun parseString(data: ByteBuffer): ByteBuffer {
    val answerByteArray = ByteArray(512) // just enough
    val startPosition = data.position()
    var currentPosition = data.position()
    while (currentPosition < data.position() + data.remaining() && data.get(currentPosition) != 0.toByte()) {
        if (data.hasRemaining()) {
            data.get(answerByteArray, currentPosition - startPosition, 1)
            currentPosition += 1
        } else {
            throw ParseException("Can't find termination 0 byte", data.position())
        }
    }
    data.get() // skip 0 byte
    return ByteBuffer.wrap(answerByteArray, 0, currentPosition - startPosition)
}

fun parseRQPacket(opCode: Short, data: ByteBuffer): Packet {
    val filename = parseString(data)
    val mode = parseString(data)
    filename.position(0)
    mode.position(0)
    return when (opCode) {
        1.toShort() -> {
            RRQPacket(filename, mode)
        }
        2.toShort() -> {
            WRQPacket(filename, mode)
        }
        else -> {
            Packet()
        }
    }
}

fun parseDataPacket(data: ByteBuffer): DataPacket {
    val blockNumber = data.short
    return DataPacket(blockNumber.toUShort(), data.slice())
}

fun parseAckPacket(data: ByteBuffer): AckPacket {
    val blockNumber = data.short
    return AckPacket(blockNumber.toUShort())
}

fun parseErrorPacket(data: ByteBuffer): ErrorPacket {
    val errorCode = data.short
    val errorMessage = parseString(data)
    errorMessage.position(0)
    return ErrorPacket(errorCode.toUShort(), errorMessage)
}

fun parseDatagramPacket(datagramPacket: DatagramPacket): Packet {
    val data: ByteBuffer = ByteBuffer.wrap(datagramPacket.data, 0, datagramPacket.length) // BIG_ENDIAN
    return try {
        when (val opCode = data.short) {
            1.toShort(), 2.toShort() -> parseRQPacket(opCode, data)
            3.toShort() -> parseDataPacket(data)
            4.toShort() -> parseAckPacket(data)
            5.toShort() -> parseErrorPacket(data)
            else -> Packet()
        }
    } catch (e: BufferUnderflowException) {
        Packet()
    }
}