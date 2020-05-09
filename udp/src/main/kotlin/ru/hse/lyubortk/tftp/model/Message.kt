package ru.hse.lyubortk.tftp.model

sealed class Message

sealed class Request(val fileName: String, val mode: Mode) : Message()

class ReadRequest(filename: String, mode: Mode) : Request(filename, mode)

class WriteRequest(fileName: String, mode: Mode) : Request(fileName, mode)

@kotlin.ExperimentalUnsignedTypes
class Data(val blockNumber: UShort, val data: ByteArray) : Message()

@kotlin.ExperimentalUnsignedTypes
class Acknowledgment(val blockNumber: UShort) : Message()

class ErrorMessage(val errorType: ErrorType, val errorMessage: String) : Message()

enum class Mode {
    NETASCII,
    OCTET
}

enum class ErrorType(val value: Short) {
    NOT_DEFINED(0),
    FILE_NOT_FOUND(1),
    ACCESS_VIOLATION(2),
    DISK_FULL(3),
    ILLEGAL_OPERATION(4),
    UNKNOWN_TRANSFER_ID(5),
    FILE_ALREADY_EXISTS(6),
    NO_SUCH_USER(7)
}

enum class MessageType(val value: Short) {
    READ_REQUEST_TYPE(1),
    WRITE_REQUEST_TYPE(2),
    DATA_TYPE(3),
    ACKNOWLEDGMENT_TYPE(4),
    ERROR_MESSAGE_TYPE(5)
}