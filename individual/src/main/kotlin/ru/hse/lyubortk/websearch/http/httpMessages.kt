package ru.hse.lyubortk.websearch.http

data class HttpRequest(
    val method: String,
    val requestTarget: String,
    val httpVersion: String,
    val headers: Map<String, List<String>>,
    val body: List<Byte>?
)

data class HttpResponse(
    val httpVersion: String,
    val statusCode: Int,
    val reasonPhrase: String,
    val headers: Map<String, List<String>>,
    val body: List<Byte>?
)