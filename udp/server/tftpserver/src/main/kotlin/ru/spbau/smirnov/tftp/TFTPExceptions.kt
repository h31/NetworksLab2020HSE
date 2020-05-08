package ru.spbau.smirnov.tftp

class FileNotFound(message: String) : Exception(message)
class FileAlreadyExists(message: String) : Exception(message)
class ReadAccessDenied(message: String) : Exception(message)
class WriteAccessDenied(message: String) : Exception(message)
class FileReadError(message: String) : Exception(message)
class FileWriteError(message: String) : Exception(message)
class UnexpectedMessage(message: String) : Exception(message)
class ErrorMessage(val errorMessage: Error) : Exception(errorMessage.errorMessage)
class NotTFTPMessage(val brokenMessage: BrokenMessage) : Exception(brokenMessage.error)