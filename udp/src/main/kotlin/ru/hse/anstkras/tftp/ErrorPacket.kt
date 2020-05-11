package ru.hse.anstkras.tftp

import java.nio.ByteBuffer

class ErrorPacket(private val errorCode: Int, private val errorMessage: ByteBuffer) : Packet {

    //    2 bytes     2 bytes      string    1 byte
    //    -----------------------------------------
    //    | Opcode |  ErrorCode |   ErrMsg   |   0  |
    //    -----------------------------------------
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(errorMessage.remaining() + 5)
        buffer.putShort(5)
        buffer.putShort(errorCode.toShort())
        buffer.put(errorMessage)
        buffer.put(0)
        return buffer
    }
}