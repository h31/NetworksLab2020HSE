package ru.hse.lyubortk.websearch.http.implementation.processor

import ru.hse.lyubortk.websearch.http.GetResponse
import ru.hse.lyubortk.websearch.http.HttpClient
import java.lang.RuntimeException
import java.net.URI
import java.time.Duration

class HttpClientRedirector(private val inner: HttpClient) : HttpClient {
    override fun get(uri: URI, timeout: Duration): GetResponse {
        var lastUri: URI = uri
        lateinit var lastResponse: GetResponse
        for (i in 0 until MAX_REDIRECTS) {
            lastResponse = inner.get(lastUri, timeout)
            if (lastResponse.statusCode() !in 300..399) {
                break
            }
            val location = lastResponse.headers()[LOCATION_HEADER]?.first() ?: throw RuntimeException("kek")
            lastUri = lastUri.resolve(location)
        }
        return lastResponse
    }

    companion object {
        private const val LOCATION_HEADER = "Location"
        private const val MAX_REDIRECTS = 5
    }
}