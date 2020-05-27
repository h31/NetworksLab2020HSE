package ru.hse.lyubortk.websearch.crawler

import org.jsoup.Jsoup
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Crawler {
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    fun getPageInfo(uri: URI): PageInfo {
        val request = HttpRequest.newBuilder().uri(uri).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val htmlString = response.body() ?: ""
        val document = Jsoup.parse(htmlString)
        val responseUri = response.uri()
        val title = document.title().ifEmpty { responseUri.toString() }
        return PageInfo(responseUri, title, document.text())
    }

    data class PageInfo(val uri: URI, val name: String, val content: String)
}