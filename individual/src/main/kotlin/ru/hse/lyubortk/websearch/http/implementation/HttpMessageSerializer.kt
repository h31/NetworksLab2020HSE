package ru.hse.lyubortk.websearch.http.implementation

object HttpMessageSerializer {
    private val CRLF = "\r\n".toByteArray().toList()
    private const val SP = ' '.toByte()

    fun serialize(response: HttpResponse): List<Byte> {
        val bytes = mutableListOf<Byte>()
        bytes.addAll(response.httpVersion.toByteArray().toList())
        bytes.add(SP)
        bytes.addAll(response.statusCode.toString().toByteArray().toList())
        bytes.add(SP)
        bytes.addAll(response.reasonPhrase.toByteArray().toList())
        bytes.addAll(CRLF)

        bytes.addAll(serializeHeaders(response.headers))
        bytes.addAll(CRLF)
        response.body?.let { bytes.addAll(it) }
        return bytes
    }

    fun serialize(request: HttpRequest): List<Byte> {
        val bytes = mutableListOf<Byte>()
        bytes.addAll(request.method.toByteArray().toList())
        bytes.add(SP)
        bytes.addAll(request.requestTarget.toByteArray().toList())
        bytes.add(SP)
        bytes.addAll(request.httpVersion.toByteArray().toList())
        bytes.addAll(CRLF)

        bytes.addAll(serializeHeaders(request.headers))
        bytes.addAll(CRLF)
        request.body?.let { bytes.addAll(it) }
        return bytes
    }

    private fun serializeHeaders(headers: Map<String, List<String>>): List<Byte> =
        headers.toList().flatMap { pair ->
            val (name, values) = pair
            values.map { Pair(name, it) }
        }.flatMap {
            val (name, value) = it
            listOf("$name: $value".toByteArray().toList(), CRLF).flatten()
        }
}