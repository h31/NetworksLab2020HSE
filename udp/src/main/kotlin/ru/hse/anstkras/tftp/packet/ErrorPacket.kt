package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class ErrorPacket(private val errorCode: Int, private val errorMessage: ByteBuffer) :
    Packet {

    //    2 bytes     2 bytes      string    1 byte
    //    -----------------------------------------
    //    | Opcode |  ErrorCode |   ErrMsg   |   0  |
    //    -----------------------------------------
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(errorMessage.remaining() + 5)
        buffer.putShort(5)
        buffer.putShort(errorCode.toUShort().toShort())
        buffer.put(errorMessage)
        buffer.put(0)
        return buffer
    }

    companion object : Parsable<Packet> {
        override fun parse(byteBuffer: ByteBuffer): Packet {
            val errorCode = byteBuffer.short
            val errorMessage = PacketParser.parseStringToByteBuffer(byteBuffer)
            return ErrorPacket(errorCode.toInt(), errorMessage)
        }

        fun getFileNotFoundPacket(): ErrorPacket {
            return ErrorPacket(1, ByteBuffer.wrap("File not found".toByteArray(StandardCharsets.US_ASCII)))
        }

        fun getUndefinedErrorPacket(message: String): ErrorPacket {
            return ErrorPacket(0, ByteBuffer.wrap(message.toByteArray(StandardCharsets.US_ASCII)))
        }
    }

    fun getErrorMessage(): String {
        return "ErrorCode: $errorCode, ${StandardCharsets.US_ASCII.decode(
            PacketParser.parseStringToByteBuffer(
                errorMessage
            )
        )}"
    }
}