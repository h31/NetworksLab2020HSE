package ru.hse.lyubortk.websearch.crawler

import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.util.RandomTreeSet
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object Crawler {
    private const val MAX_SET_SIZE = 500
    private const val TIMEOUT_MILLIS: Long = 10_000

    private val log = LoggerFactory.getLogger(Crawler::class.java)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun getPageStream(uri: URI): PageStream = PageStream(uri)

    /**
     * An Iterator-like class which downloads pages and adds all links to the queue.
     * Does not implement the Iterator interface because Iterators are used with collections
     * and no one expects an Iterator to send HTTP requests .
     */
    class PageStream(startURI: URI) {
        private val visitedUris: MutableSet<URI> = mutableSetOf()
        private val uriQueue: RandomTreeSet<URI> = RandomTreeSet<URI>().also { it.add(startURI) }

        fun hasNext(): Boolean = uriQueue.isNotEmpty()

        fun next(): PageFetchResult {
            if (uriQueue.isEmpty()) {
                throw NoSuchElementException()
            }
            val uri: URI = uriQueue.getRandom()
            uriQueue.remove(uri)

            try {
                val request = HttpRequest.newBuilder().uri(uri).GET().timeout(Duration.ofSeconds(TIMEOUT_MILLIS)).build()
                log.info("Sending GET to $uri")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val responseUri = response.uri()
                visitedUris.add(uri)
                visitedUris.add(responseUri)
                if (!response.headers().allValues("Content-Type").any { it.startsWith("text") }) {
                    return PageFetchResult.NotTextPage(responseUri)
                }

                val htmlString = response.body() ?: ""
                val document = Jsoup.parse(htmlString)
                val title = document.title().ifEmpty { responseUri.toString() }

                document.select("a")
                    .asSequence()
                    .mapNotNull { it.attr("href") }
                    .filterNot { it.startsWith("#") }
                    .mapNotNull {
                        try {
                            responseUri.resolve(it)
                        } catch (e: Exception) {
                            null
                        }
                    }.filterNot { visitedUris.contains(it) }
                    .toList()
                    .shuffled()
                    .forEach { uriQueue.add(it) }

                while (uriQueue.size() > MAX_SET_SIZE) {
                    uriQueue.remove(uriQueue.getRandom())
                }

                return PageFetchResult.TextPage(responseUri, title, document.text())
            } catch (e: Exception) {
                log.error("Exception while trying to get page from $uri")
                return PageFetchResult.RequestError(uri, e)
            }
        }
    }

    sealed class PageFetchResult(val uri: URI) {
        class TextPage(uri: URI, val name: String, val content: String) : PageFetchResult(uri)
        class NotTextPage(uri: URI) : PageFetchResult(uri)
        class RequestError(uri: URI, val exception: Exception) : PageFetchResult(uri)
    }
}