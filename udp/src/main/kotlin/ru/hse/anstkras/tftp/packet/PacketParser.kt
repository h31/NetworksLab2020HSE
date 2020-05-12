package ru.hse.anstkras.tftp.packet

import ru.hse.anstkras.tftp.Client
import java.nio.ByteBuffer

class PacketParser {
    companion object {
        fun parsePacket(buffer: ByteBuffer): Packet {
            val opCode = buffer.short
            return when (opCode) {
                1.toShort() -> RRQPacket.parse(buffer)
                2.toShort() -> WRQPacket.parse(buffer)
                3.toShort() -> DataPacket.parse(buffer)
                4.toShort() -> AckPacket.parse(buffer)
                5.toShort() -> ErrorPacket.parse(buffer)
                else -> error("Wrong opCode")
            }
        }

        fun parseStringToByteBuffer(buffer: ByteBuffer): ByteBuffer {
            val resultBuffer = ByteBuffer.allocate(Client.bufferCapacity)
            while (buffer.hasRemaining()) {
                val b = buffer.get()
                if (b == 0.toByte()) {
                    resultBuffer.flip()
                    return resultBuffer
                }
                resultBuffer.put(b)
            }
            error("No 0 at the end of string")
        }
    }
}