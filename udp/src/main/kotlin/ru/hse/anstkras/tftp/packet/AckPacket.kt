package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer

class AckPacket(private val blockNum : Int) : Packet {

    //    2 bytes     2 bytes
    //    ---------------------
    //    | Opcode |   Block #  |
    //    ---------------------
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(4)
        buffer.putShort(4)
        buffer.putShort(blockNum.toShort())
        return buffer
    }
}