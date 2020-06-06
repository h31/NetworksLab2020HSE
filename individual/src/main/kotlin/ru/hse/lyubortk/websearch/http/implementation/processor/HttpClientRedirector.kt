package ru.hse.lyubortk.websearch.http.implementation.processor

import ru.hse.lyubortk.websearch.config.RedirectorConfig
import ru.hse.lyubortk.websearch.http.GetResponse
import ru.hse.lyubortk.websearch.http.HttpClient
import ru.hse.lyubortk.websearch.http.implementation.NoLocationHeaderException
import java.net.URI
import java.time.Duration

class HttpClientRedirector(private val inner: HttpClient, private val config: RedirectorConfig) : HttpClient {
    override fun get(uri: URI, timeout: Duration): GetResponse {
        var lastUri: URI = uri
        lateinit var lastResponse: GetResponse
        for (i in 0 until config.maxRedirects) {
            lastResponse = inner.get(lastUri, timeout)
            if (lastResponse.statusCode() !in 300..399) {
                break
            }
            val location =
                lastResponse.headers()[LOCATION_HEADER]?.first() ?: throw NoLocationHeaderException(
                    "Location header was not present in response with code ${lastResponse.statusCode()}"
                )
            lastUri = lastUri.resolve(location)
        }
        return lastResponse
    }
}