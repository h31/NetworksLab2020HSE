package ru.hse.lyubortk.websearch

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.SearchApi
import ru.hse.lyubortk.websearch.config.*
import ru.hse.lyubortk.websearch.core.Searcher
import ru.hse.lyubortk.websearch.crawler.Crawler
import ru.hse.lyubortk.websearch.http.adapters.JavalinHttpServerAdapter
import ru.hse.lyubortk.websearch.http.adapters.JdkHttpClientAdapter
import ru.hse.lyubortk.websearch.http.implementation.connector.HttpClientConnector
import ru.hse.lyubortk.websearch.http.implementation.connector.HttpServerConnector
import ru.hse.lyubortk.websearch.http.implementation.processor.HttpClientMessageProcessor
import ru.hse.lyubortk.websearch.http.implementation.processor.HttpClientRedirector
import ru.hse.lyubortk.websearch.http.implementation.processor.HttpServerMessageProcessor
import java.net.http.HttpClient

private fun printUsage() = println(
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

    val config = ConfigFactory.parseResources("application.conf")
    val searcherConfig = config.extract<SearcherConfig>("searcher")
    val serverConnectorConfig = config.extract<ServerConnectorConfig>("server-connector")
    val crawlerConfig = config.extract<CrawlerConfig>("crawler")
    val redirectorConfig = config.extract<RedirectorConfig>("redirector")
    val clientConnectorConfig = config.extract<ClientConnectorConfig>("client-connector")

    val server = when (serverParam) {
        "javalin" -> JavalinHttpServerAdapter(Javalin.create().start(port))
        "custom" -> {
            val requestProcessor = HttpServerMessageProcessor()
            HttpServerConnector(port, requestProcessor, serverConnectorConfig)
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
                .connectTimeout(clientConnectorConfig.connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
            JdkHttpClientAdapter(jdkClient)
        }
        "custom" -> {
            val connector = HttpClientConnector(clientConnectorConfig)
            val messageProcessor = HttpClientMessageProcessor(connector)
            HttpClientRedirector(messageProcessor, redirectorConfig)
        }
        else -> {
            printUsage()
            return
        }
    }

    val crawler = Crawler(client, crawlerConfig)
    val searcher = Searcher(crawler, searcherConfig)
    SearchApi(server, searcher)
}