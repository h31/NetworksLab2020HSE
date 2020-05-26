package ru.hse.lyubortk.websearch.api

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.html.AddedToIndexPage
import ru.hse.lyubortk.websearch.api.html.FrontPage
import ru.hse.lyubortk.websearch.core.Searcher

class RequestHandler(server: Javalin, searcher: Searcher) {
    init {
        server.get("/") { ctx -> ctx.html(FrontPage.createHtml(null)) }
        server.get("/search") { ctx ->
            val query = ctx.queryParam("text")
            val searchResult = query?.let { searcher.search(it) } ?: emptyList()
            ctx.html(FrontPage.createHtml(searchResult.map { FrontPage.SearchResult(it.url, it.name) }))
        }
        server.post("/add") { ctx ->
            val url = ctx.formParam("url")
            url?.let { searcher.addToIndex(it) }
            ctx.html(AddedToIndexPage.html)
        }
    }
}