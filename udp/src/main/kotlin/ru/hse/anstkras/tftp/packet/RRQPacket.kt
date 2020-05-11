package ru.hse.anstkras.tftp.packet

import ru.hse.anstkras.tftp.TFTPMode
import java.nio.ByteBuffer

class RRQPacket(private val fileName: String, private val mode: TFTPMode) :
    Packet {

    //           2 bytes    string   1 byte     string   1 byte
    //           -----------------------------------------------
    //    RRQ   | 01 |  Filename  |   0  |    Mode    |   0  |
    //           -----------------------------------------------
    override fun getBytesRepresentation(): ByteBuffer {
        // TODO кодировки
        val fileNameByteArray = fileName.toByteArray()
        val modeByteArray = mode.modeStringRepresentation().toByteArray()
        val buffer = ByteBuffer.allocate(fileNameByteArray.size + modeByteArray.size + 4)
        buffer.putShort(1)
        buffer.put(fileNameByteArray)
        buffer.put(0)
        buffer.put(modeByteArray)
        buffer.put(0)
        return buffer
    }
}