package ru.hse.lyubortk.websearch.api

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.html.FrontPage
import ru.hse.lyubortk.websearch.api.html.ResultPage
import ru.hse.lyubortk.websearch.core.Searcher
import java.net.URL

class RequestHandler(server: Javalin, searcher: Searcher) {
    init {
        server.get("/") {
                ctx -> ctx.html(FrontPage.createHtml(null))
        }

        server.get("/search") { ctx ->
            val query = ctx.queryParam("text")
            val searchResult = query?.let { searcher.search(it) } ?: emptyList()
            ctx.html(FrontPage.createHtml(searchResult.map { FrontPage.SearchResult(it.url, it.name) }))
        }

        server.post("/add") { ctx ->
            try {
                val urlString = ctx.formParam("url")
                if (urlString == null) {
                    ctx.html(ResultPage.createHtml("ERROR: url parameter was not found"))
                    return@post
                }
                val url = URL(urlString)
                url.toURI() // throws if url is not formatted according to RFC2396
                searcher.addToIndex(url)
                ctx.html(ResultPage.createHtml("Page was successfully added to index"))
            } catch (e: Exception) {
                ctx.html(ResultPage.createHtml("ERROR: ${e.message ?: "INTERNAL ERROR"}"))
            }
        }
    }
}