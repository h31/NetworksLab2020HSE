package ru.hse.lyubortk.tftp.model

import ru.hse.lyubortk.tftp.TFTP_PACKET_MAX_LENGTH
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

sealed class SerializationException(msg: String) : RuntimeException(msg)
class InvalidNameException : SerializationException("Check that names contain only non-zero ascii characters.")
class ResultTooLongException : SerializationException("Resulting byte array is too long for TFTP")

object Serializer {
    fun serialize(message: Message): ByteArray {
        val bytes = message.getTypeCode().bytes().toMutableList()
        when (message) {
            is Request -> serializeRequest(bytes, message)
            is Data -> serializeData(bytes, message)
            is Acknowledgment -> serializeAcknowledgment(bytes, message)
            is ErrorMessage -> serializeError(bytes, message)
        }
        if (bytes.size > TFTP_PACKET_MAX_LENGTH) {
            throw ResultTooLongException()
        }
        return bytes.toByteArray()
    }

    private fun serializeRequest(buffer: MutableList<Byte>, message: Request) {
        validateString(message.fileName)
        buffer.addAll(message.fileName.bytes().toList())
        buffer.add(ZERO_BYTE)
        buffer.addAll(message.mode.name.bytes().toList())
        buffer.add(ZERO_BYTE)
    }

    private fun serializeData(buffer: MutableList<Byte>, message: Data) {
        buffer.addAll(message.blockNumber.bytes().toList())
        buffer.addAll(message.data.toList())
    }

    private fun serializeAcknowledgment(buffer: MutableList<Byte>, message: Acknowledgment) {
        buffer.addAll(message.blockNumber.bytes().toList())
    }

    private fun serializeError(buffer: MutableList<Byte>, message: ErrorMessage) {
        validateString(message.errorMessage)
        buffer.addAll(message.errorType.value.bytes().toList())
        buffer.addAll(message.errorMessage.bytes().toList())
        buffer.add(ZERO_BYTE)
    }
}

private const val ZERO_BYTE: Byte = 0

private fun validateString(str: String) {
    if (!str.isAscii() || !str.hasNoZeroChars()) {
        throw InvalidNameException()
    }
}

private fun Short.bytes(): ByteArray =
    ByteBuffer.allocate(Short.SIZE_BYTES)
        .putShort(this)
        .array()

private fun String.bytes(): ByteArray = this.toByteArray(StandardCharsets.US_ASCII)

private fun String.isAscii(): Boolean =
    StandardCharsets.US_ASCII.newEncoder()
        .canEncode(this)

private fun String.hasNoZeroChars(): Boolean = this.all { it.toInt() != 0 }

private fun Message.getTypeCode(): Short =
    when (this) {
        is ReadRequest -> MessageType.READ_REQUEST_TYPE.value
        is WriteRequest -> MessageType.WRITE_REQUEST_TYPE.value
        is Data -> MessageType.DATA_TYPE.value
        is Acknowledgment -> MessageType.ACKNOWLEDGMENT_TYPE.value
        is ErrorMessage -> MessageType.ERROR_MESSAGE_TYPE.value
    }
