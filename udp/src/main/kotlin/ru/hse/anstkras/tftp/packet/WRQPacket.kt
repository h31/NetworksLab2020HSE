package ru.hse.anstkras.tftp.packet

import ru.hse.anstkras.tftp.TFTPMode
import java.nio.ByteBuffer

class WRQPacket(private val fileName: ByteBuffer, private val mode: TFTPMode) :
    Packet {

    //           2 bytes    string   1 byte     string   1 byte
    //           -----------------------------------------------
    //    WRQ   | 02 |  Filename  |   0  |    Mode    |   0  |
    //           -----------------------------------------------
    override fun getBytesRepresentation(): ByteBuffer {
        val modeByteArray = mode.modeStringRepresentation().toByteArray()
        val buffer = ByteBuffer.allocate(fileName.remaining() + modeByteArray.size + 4)
        buffer.putShort(2)
        buffer.put(fileName.array())
        buffer.put(0)
        buffer.put(modeByteArray)
        buffer.put(0)
        return buffer
    }

    companion object : Parsable<Packet> {
        override fun parse(byteBuffer: ByteBuffer): Packet {
            val fileName = PacketParser.parseStringToByteBuffer(byteBuffer)
            val mode = TFTPMode.valueOf(PacketParser.parseStringToByteBuffer(byteBuffer).toString().toUpperCase())

            return WRQPacket(fileName, mode)
        }
    }
}