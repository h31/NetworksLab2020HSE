package ru.hse.lyubortk.websearch.http

import java.net.URI
import java.time.Duration

interface HttpClient {
    fun get(uri: URI, timeout: Duration = Duration.ofSeconds(10)): GetResponse
}

interface GetResponse {
    fun responseUri(): URI
    fun headers(): Map<String, List<String>>
    fun body(): String?
}