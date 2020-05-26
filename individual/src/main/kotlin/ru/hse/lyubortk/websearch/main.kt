package ru.hse.lyubortk.websearch

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.RequestHandler
import ru.hse.lyubortk.websearch.api.html.FrontPage
import ru.hse.lyubortk.websearch.core.Searcher

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

    when (args[1]) {
        "javalin" -> Unit
        "custom" -> Unit
        else -> {
            printUsage()
            return
        }
    }

    val javalin = Javalin.create().start(port)
    RequestHandler(javalin, Searcher())
}