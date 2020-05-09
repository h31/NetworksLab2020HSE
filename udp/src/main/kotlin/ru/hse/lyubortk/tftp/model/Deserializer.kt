package ru.hse.lyubortk.tftp.model

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

sealed class DeserializationException(msg: String) : RuntimeException(msg)
class CorruptMessage : DeserializationException("Cannot parse message")
class UnknownOperation(code: Short) : DeserializationException("Operation code $code is not supported")
class UnknownMode(str: String) : DeserializationException("Mode $str is not supported")
class UnknownErrorCode(code: Short) : DeserializationException("Error code $code is not supported")

@ExperimentalUnsignedTypes
object Deserializer {
    fun deserialize(bytes: ByteArray): Message {
        if (bytes.size < Short.SIZE_BYTES) {
            throw CorruptMessage()
        }
        val messageTypeCode = bytes.copyOf(2).toShort()
        val messageType = MessageType.values().firstOrNull { it.value == messageTypeCode }
        messageType ?: throw UnknownOperation(messageTypeCode)

        val bytesWithoutType = bytes.drop(Short.SIZE_BYTES).toByteArray()
        return when (messageType) {
            MessageType.READ_REQUEST_TYPE -> deserializeRequest(bytesWithoutType) { filename, mode ->
                ReadRequest(filename, mode)
            }
            MessageType.WRITE_REQUEST_TYPE -> deserializeRequest(bytesWithoutType) { filename, mode ->
                WriteRequest(filename, mode)
            }
            MessageType.DATA_TYPE -> deserializeData(bytesWithoutType)
            MessageType.ACKNOWLEDGMENT_TYPE -> deserializeAcknowledgment(bytesWithoutType)
            MessageType.ERROR_MESSAGE_TYPE -> deserializeErrorMessage(bytesWithoutType)
        }
    }

    private fun deserializeRequest(bytes: ByteArray, constructor: (String, Mode) -> Request): Request {
        val firstZeroByteIndex = bytes.indexOf(ZERO_BYTE)
        if (firstZeroByteIndex == -1) {
            throw CorruptMessage()
        }
        val secondZeroByteIndex = bytes.drop(firstZeroByteIndex + 1).indexOf(ZERO_BYTE)
        if (secondZeroByteIndex == -1) {
            throw CorruptMessage()
        }

        val fileNameSubArray = bytes.copyOf(firstZeroByteIndex)
        val modeSubArray = bytes.copyOfRange(firstZeroByteIndex + 1, firstZeroByteIndex + 1 + secondZeroByteIndex)

        val fileName = String(fileNameSubArray, StandardCharsets.US_ASCII)
        val modeString = String(modeSubArray, StandardCharsets.US_ASCII)
        val mode = Mode.values().firstOrNull { it.name.equals(modeString, true) }
        mode ?: throw UnknownMode(modeString)
        return constructor(fileName, mode)
    }

    private fun deserializeData(bytes: ByteArray): Data {
        val blockNumber = bytes.copyOf(Short.SIZE_BYTES).toUShort()
        val data = bytes.drop(Short.SIZE_BYTES).toByteArray()
        return Data(blockNumber, data)
    }

    private fun deserializeAcknowledgment(bytes: ByteArray): Acknowledgment {
        val blockNumber = bytes.copyOf(Short.SIZE_BYTES).toUShort()
        return Acknowledgment(blockNumber)
    }

    private fun deserializeErrorMessage(bytes: ByteArray): ErrorMessage {
        val errorCode = bytes.copyOf(Short.SIZE_BYTES).toShort()
        val errorType = ErrorType.values().firstOrNull { it.value == errorCode }
        errorType ?: throw UnknownErrorCode(errorCode)

        val firstZeroByteIndex = bytes.drop(Short.SIZE_BYTES).indexOf(ZERO_BYTE)
        if (firstZeroByteIndex == -1) {
            throw CorruptMessage()
        }
        val errorMessageSubArray = bytes.copyOfRange(Short.SIZE_BYTES, firstZeroByteIndex + Short.SIZE_BYTES)
        val errorMessage = String(errorMessageSubArray, StandardCharsets.US_ASCII)
        return ErrorMessage(errorType, errorMessage)
    }
}

private const val ZERO_BYTE: Byte = 0

private fun ByteArray.toShort(): Short =
    ByteBuffer.allocate(Short.SIZE_BYTES)
        .put(this)
        .flip()
        .short

@ExperimentalUnsignedTypes
private fun ByteArray.toUShort(): UShort =
    ByteBuffer.allocate(Short.SIZE_BYTES)
        .put(this)
        .flip()
        .short
        .toUShort()
