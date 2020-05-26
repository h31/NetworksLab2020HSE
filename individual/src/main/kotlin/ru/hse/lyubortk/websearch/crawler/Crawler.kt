package ru.hse.lyubortk.websearch.crawler

import org.jsoup.Jsoup
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Crawler {
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    fun getPageInfo(url: URL): PageInfo {
        val request = HttpRequest.newBuilder().uri(url.toURI()).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val htmlString = response.body() ?: ""
        val document = Jsoup.parse(htmlString)
        val title = document.title().ifEmpty { url.toString() }
        return PageInfo(title, document.text())
    }

    data class PageInfo(val name: String, val content: String)
}