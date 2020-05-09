package ru.spbau.smirnov.tftp.client

enum class ErrorCode(val code: Int) {
    NOT_DEFINED(0),
    FILE_NOT_FOUND(1),
    ACCESS_VIOLATION(2),
    DISK_FULL(3),
    ILLEGAL_OPERATION(4),
    UNKNOWN_TRANSFER_ID(5),
    FILE_EXISTS(6),
    NO_SUCH_USER(7);

    companion object {
        fun byCode(code: Int): ErrorCode {
            return when (code) {
                0 -> NOT_DEFINED
                1 -> FILE_NOT_FOUND
                2 -> ACCESS_VIOLATION
                3 -> DISK_FULL
                4 -> ILLEGAL_OPERATION
                5 -> UNKNOWN_TRANSFER_ID
                6 -> FILE_EXISTS
                7 -> NO_SUCH_USER
                else -> throw IllegalArgumentException("No such code")
            }
        }
    }
}