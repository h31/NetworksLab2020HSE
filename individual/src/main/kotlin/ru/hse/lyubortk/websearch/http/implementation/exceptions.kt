package ru.hse.lyubortk.websearch.http.implementation

class UnknownSchemeException(msg: String) : RuntimeException(msg)
class ResponseParseErrorException(msg: String) : RuntimeException(msg)
class UnsupportedEncodingException(msg: String) : RuntimeException(msg)
class NoLocationHeaderException(msg: String) : RuntimeException(msg)