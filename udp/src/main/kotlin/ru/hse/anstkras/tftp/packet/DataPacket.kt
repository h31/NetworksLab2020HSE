package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer

class DataPacket(private val blockNum: Int, private val data: ByteBuffer) :
    Packet {

    //    2 bytes     2 bytes      n bytes
    //    ----------------------------------
    //    | Opcode |   Block #  |   Data     |
    //    ----------------------------------
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(4 + data.remaining())
        buffer.putShort(3)
        buffer.putShort(blockNum.toShort())
        buffer.put(data)
        return buffer
    }
}