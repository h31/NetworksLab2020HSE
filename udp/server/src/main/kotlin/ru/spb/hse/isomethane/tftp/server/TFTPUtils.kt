package ru.spb.hse.isomethane.tftp.server

import ru.spb.hse.isomethane.tftp.server.TFTPUtils.ACK_CODE
import ru.spb.hse.isomethane.tftp.server.TFTPUtils.DATA_CODE
import ru.spb.hse.isomethane.tftp.server.TFTPUtils.ERR_CODE
import ru.spb.hse.isomethane.tftp.server.TFTPUtils.READ_CODE
import ru.spb.hse.isomethane.tftp.server.TFTPUtils.WRITE_CODE
import java.io.ByteArrayOutputStream

sealed class TFTPMessage

class ReadRequest(val fileName: String, val mode: String) : TFTPMessage()

class WriteRequest(val fileName: String, val mode: String) : TFTPMessage()

class Data(val block: Int, val data: ByteArray) : TFTPMessage()

class Acknowledgement(val block: Int) : TFTPMessage()

class ErrorMessage(val errorNo: Int, val errorMessage: String) : TFTPMessage()

object IncorrectMessage : TFTPMessage()

fun readTFTP(byteArray: ByteArray, length: Int): TFTPMessage {
    val reader = TFTPReaderHelper(byteArray, length)

    return when (reader.readInt()) {
        READ_CODE -> {
            val fileName = reader.readString()
            val mode = reader.readString()
            ReadRequest(fileName, mode)
        }
        WRITE_CODE -> {
            val fileName = reader.readString()
            val mode = reader.readString()
            WriteRequest(fileName, mode)
        }
        DATA_CODE -> {
            val block = reader.readInt()
            val data = reader.readData()
            Data(block, data)
        }
        ACK_CODE -> {
            val block = reader.readInt()
            Acknowledgement(block)
        }
        ERR_CODE -> {
            val errorNo = reader.readInt()
            val errorMessage = reader.readString()
            ErrorMessage(errorNo, errorMessage)
        }
        else -> IncorrectMessage
    }
}

fun writeTFTP(tftp: TFTPMessage): ByteArray {
    val writer = TFTPWriterHelper()
    when (tftp) {
        is ReadRequest -> {
            writer.writeInt(READ_CODE)
            writer.writeString(tftp.fileName)
            writer.writeString(tftp.mode)
        }
        is WriteRequest -> {
            writer.writeInt(WRITE_CODE)
            writer.writeString(tftp.fileName)
            writer.writeString(tftp.mode)
        }
        is Data -> {
            writer.writeInt(DATA_CODE)
            writer.writeInt(tftp.block)
            writer.writeData(tftp.data)
        }
        is Acknowledgement -> {
            writer.writeInt(ACK_CODE)
            writer.writeInt(tftp.block)
        }
        is ErrorMessage -> {
            writer.writeInt(ERR_CODE)
            writer.writeInt(tftp.errorNo)
            writer.writeString(tftp.errorMessage)
        }
    }
    return writer.getResult()
}

private class TFTPReaderHelper(private val byteArray: ByteArray, private val length: Int) {
    private var cursor = 0
    private val zeros = byteArray
        .mapIndexed { index, byte ->
            if (byte == 0.toByte()) index else null
        }.filterNotNull()

    fun readInt(): Int {
        val result = byteArray[cursor].toInt().and(0xFF).shl(8) + byteArray[cursor + 1].toInt().and(0xFF)
        cursor += 2
        return result
    }

    fun readString(): String {
        val end = zeros.find { it >= cursor }!!
        val result = byteArray.sliceArray(IntRange(cursor, end - 1))
        cursor = end + 1
        return String(result)
    }

    fun readData(): ByteArray {
        val result = byteArray.sliceArray(IntRange(cursor, length - 1))
        cursor = length
        return result
    }
}

private class TFTPWriterHelper {
    private val output = ByteArrayOutputStream()

    fun writeInt(value: Int) {
        output.write(value.shr(8))
        output.write(value)
    }

    fun writeString(value: String) {
        output.writeBytes(value.toByteArray())
        output.write(0)
    }

    fun writeData(value: ByteArray) {
        output.writeBytes(value)
    }

    fun getResult(): ByteArray = output.toByteArray()
}

private object TFTPUtils {
    const val READ_CODE = 1
    const val WRITE_CODE = 2
    const val DATA_CODE = 3
    const val ACK_CODE = 4
    const val ERR_CODE = 5
}
