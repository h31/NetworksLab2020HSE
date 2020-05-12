package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer

class AckPacket(private val blockNum: Int) : Packet {

    //    2 bytes     2 bytes
    //    ---------------------
    //    | Opcode |   Block #  |
    //    ---------------------
    @ExperimentalUnsignedTypes
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(4)
        buffer.putShort(4)
        buffer.putShort(blockNum.toUShort().toShort())
        return buffer
    }

    companion object : Parsable<Packet> {
        override fun parse(byteBuffer: ByteBuffer): Packet {
            val blockNum = byteBuffer.short
            return AckPacket(blockNum.toInt())
        }
    }
}