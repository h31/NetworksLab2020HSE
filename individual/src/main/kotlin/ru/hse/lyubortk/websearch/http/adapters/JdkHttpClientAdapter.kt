package ru.hse.lyubortk.websearch.http.adapters

import ru.hse.lyubortk.websearch.http.GetResponse
import ru.hse.lyubortk.websearch.http.HttpClient
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class JdkHttpClientAdapter(private val client: java.net.http.HttpClient) : HttpClient {
    override fun get(uri: URI, timeout: Duration): GetResponse {
        val request = HttpRequest.newBuilder().uri(uri).GET().timeout(timeout).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return object : GetResponse {
            override fun statusCode(): Int = response.statusCode()
            override fun responseUri(): URI = response.uri()
            override fun headers(): Map<String, List<String>> = response.headers().map()
            override fun body(): String? = response.body()
        }
    }

}