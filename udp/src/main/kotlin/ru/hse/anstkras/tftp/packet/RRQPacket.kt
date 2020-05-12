package ru.hse.anstkras.tftp.packet

import ru.hse.anstkras.tftp.TFTPMode
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class RRQPacket(val fileName: ByteBuffer, val mode: TFTPMode) :
    Packet {

    //           2 bytes    string   1 byte     string   1 byte
    //           -----------------------------------------------
    //    RRQ   | 01 |  Filename  |   0  |    Mode    |   0  |
    //           -----------------------------------------------
    override fun getBytesRepresentation(): ByteBuffer {
        val modeByteArray = mode.modeStringRepresentation().toByteArray()
        val buffer = ByteBuffer.allocate(fileName.remaining() + modeByteArray.size + 4)
        buffer.putShort(1)
        buffer.put(fileName.array())
        buffer.put(0)
        buffer.put(modeByteArray)
        buffer.put(0)
        return buffer
    }

    companion object : Parsable<Packet> {
        override fun parse(byteBuffer: ByteBuffer): Packet {
            val fileName = PacketParser.parseStringToByteBuffer(byteBuffer)
            val mode = TFTPMode.valueOf(StandardCharsets.US_ASCII.decode(PacketParser.parseStringToByteBuffer(byteBuffer)).toString().toUpperCase())

            return RRQPacket(fileName, mode)
        }
    }
}