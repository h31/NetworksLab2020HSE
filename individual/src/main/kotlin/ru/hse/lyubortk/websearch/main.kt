package ru.hse.lyubortk.websearch

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.SearchApi
import ru.hse.lyubortk.websearch.core.Searcher
import ru.hse.lyubortk.websearch.http.HttpServer
import ru.hse.lyubortk.websearch.http.JavalinEndpointBinderAdapter
import ru.hse.lyubortk.websearch.http.RequestProcessor

fun printUsage() = println(
    """
    |Usage:
    |javalin http server and client: ./gradlew run --args='PORT javalin'
    |custom http server and client: ./gradlew run --args='PORT custom'
""".trimMargin()
)

fun main(args: Array<String>) {
    if (args.size != 2) {
        printUsage()
        return
    }
    val port = args[0].toIntOrNull()
    if (port == null) {
        printUsage()
        return
    }

    val server = when (args[1]) {
        "javalin" -> JavalinEndpointBinderAdapter(Javalin.create().start(port))
        "custom" -> {
            val requestProcessor = RequestProcessor()
            val httpServer = HttpServer(port, requestProcessor)
            requestProcessor
        }
        else -> {
            printUsage()
            return
        }
    }

    SearchApi(server, Searcher())
}