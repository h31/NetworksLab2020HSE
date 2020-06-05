package ru.hse.lyubortk.websearch.http.implementation

object HttpResponseSerializer {
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

        response.headers.toList().flatMap { pair ->
            val (name, values) = pair
            values.map { Pair(name, it) }
        }.forEach {
            val (name, value) = it
            bytes.addAll("$name: $value".toByteArray().toList())
            bytes.addAll(CRLF)
        }
        bytes.addAll(CRLF)
        response.body?.let { bytes.addAll(it) }
        return bytes
    }
}