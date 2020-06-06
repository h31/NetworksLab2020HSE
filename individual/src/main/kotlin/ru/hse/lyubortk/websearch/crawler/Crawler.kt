package ru.hse.lyubortk.websearch.crawler

import org.eclipse.jetty.util.URIUtil
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.http.HttpClient
import ru.hse.lyubortk.websearch.util.RandomTreeSet
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.time.Duration

class Crawler(private val httpClient: HttpClient) {
    private val log = LoggerFactory.getLogger(Crawler::class.java)

    fun getPageStream(uri: URI): PageStream = PageStream(uri)

    /**
     * An Iterator-like class which downloads pages and adds all links to the queue.
     * Does not implement the Iterator interface because Iterators are used with collections
     * and no one expects an Iterator to send HTTP requests .
     */
    inner class PageStream(startURI: URI) {
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
                log.info("Sending GET to $uri")
                val response = httpClient.get(uri, Duration.ofMillis(TIMEOUT_MILLIS))
                val responseUri = response.responseUri()
                visitedUris.add(uri)
                visitedUris.add(responseUri)
                val isText = response.headers()
                    .getOrDefault(CONTENT_TYPE_HEADER, listOf())
                    .any { it.startsWith("text") }

                if (!isText) {
                    return PageFetchResult.NotTextPage(responseUri)
                }

                val htmlString = response.body() ?: ""
                val document = Jsoup.parse(htmlString)
                val title = document.title().ifEmpty { responseUri.toString() }

                document.select("a")
                    .asSequence()
                    .mapNotNull { it.attr("href") }
                    .filterNot { it.startsWith("#") }
                    .map { URIUtil.encodePath(it) }
                    .mapNotNull {
                        try {
                            responseUri.resolve(it)
                        } catch (e: Exception) {
                            log.info("invalid uri $it in page $uri", e)
                            null
                        }
                    }
                    .filterNot { visitedUris.contains(it) }
                    .toList()
                    .shuffled()
                    .forEach { uriQueue.add(it) }

                while (uriQueue.size() > MAX_SET_SIZE) {
                    uriQueue.remove(uriQueue.getRandom())
                }

                return PageFetchResult.TextPage(responseUri, title, document.text())
            } catch (e: Exception) {
                log.error("Exception while trying to get page from $uri", e)
                return PageFetchResult.RequestError(uri, e)
            }
        }
    }

    sealed class PageFetchResult(val uri: URI) {
        class TextPage(uri: URI, val name: String, val content: String) : PageFetchResult(uri)
        class NotTextPage(uri: URI) : PageFetchResult(uri)
        class RequestError(uri: URI, val exception: Exception) : PageFetchResult(uri)
    }

    companion object {
        private const val MAX_SET_SIZE = 500
        private const val TIMEOUT_MILLIS: Long = 10_000
        private const val CONTENT_TYPE_HEADER = "Content-Type"
    }
}