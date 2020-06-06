package ru.hse.lyubortk.websearch

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.SearchApi
import ru.hse.lyubortk.websearch.core.Searcher
import ru.hse.lyubortk.websearch.crawler.Crawler
import ru.hse.lyubortk.websearch.http.adapters.JavalinEndpointBinderAdapter
import ru.hse.lyubortk.websearch.http.adapters.JdkHttpClientAdapter
import ru.hse.lyubortk.websearch.http.implementation.HttpClientImpl
import ru.hse.lyubortk.websearch.http.implementation.HttpServer
import ru.hse.lyubortk.websearch.http.implementation.RequestProcessor
import java.net.http.HttpClient
import java.time.Duration

fun printUsage() = println(
    """
    |Usage:
    |./gradlew run --args='PORT (javalin/custom) (jdk/custom)'
""".trimMargin()
)

fun main(args: Array<String>) {
    if (args.size != 3) {
        printUsage()
        return
    }
    val port = args[0].toIntOrNull()
    if (port == null) {
        printUsage()
        return
    }
    val serverParam = args[1].toLowerCase()
    val clientParam = args[2].toLowerCase()

    val server = when (serverParam) {
        "javalin" -> JavalinEndpointBinderAdapter(Javalin.create().start(port))
        "custom" -> {
            val requestProcessor = RequestProcessor()
            HttpServer(port, requestProcessor)
            requestProcessor
        }
        else -> {
            printUsage()
            return
        }
    }

    val client = when (clientParam) {
        "jdk" -> {
            val jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10_000)) //TODO: fixme
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
            JdkHttpClientAdapter(jdkClient)
        }
        "custom" -> {
            HttpClientImpl(Duration.ofMillis(10_000))
        }
        else -> {
            printUsage()
            return
        }
    }

    val crawler = Crawler(client)
    val searcher = Searcher(crawler)
    SearchApi(server, searcher)
}