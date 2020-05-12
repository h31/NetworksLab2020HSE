package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer

class DataPacket(val blockNum: Int, val data: ByteBuffer) :
    Packet {

    //    2 bytes     2 bytes      n bytes
    //    ----------------------------------
    //    | Opcode |   Block #  |   Data     |
    //    ----------------------------------
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getBytesRepresentation(): ByteBuffer {
        val buffer = ByteBuffer.allocate(4 + data.remaining())
        buffer.putShort(3)
        buffer.putShort(blockNum.toUShort().toShort())
        buffer.put(data)
        return buffer
    }

    companion object : Parsable<Packet> {
        override fun parse(byteBuffer: ByteBuffer): DataPacket {
            val blockNum = byteBuffer.short
            val data = byteBuffer.slice()
            return DataPacket(blockNum.toInt(), data)
        }
    }
}