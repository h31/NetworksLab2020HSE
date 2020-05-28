package ru.hse.lyubortk.websearch.api

import io.javalin.Javalin
import ru.hse.lyubortk.websearch.api.html.FrontPage
import ru.hse.lyubortk.websearch.api.html.ResultPage
import ru.hse.lyubortk.websearch.core.Searcher
import ru.hse.lyubortk.websearch.core.Searcher.Companion.StartIndexingResult.Refused
import ru.hse.lyubortk.websearch.core.Searcher.Companion.StartIndexingResult.Started
import java.net.URL

class RequestHandler(server: Javalin, searcher: Searcher) {
    init {
        server.get(BASE_PATH) { ctx ->
            ctx.html(FrontPage.createHtml(searcher.getStats(), null))
        }

        server.get(SEARCH_PATH) { ctx ->
            try {
                val query = ctx.queryParam(SEARCH_QUERY_PARAM)
                if (query == null) {
                    ctx.html(ResultPage.createHtml("ERROR: $SEARCH_QUERY_PARAM parameter was not found"))
                    return@get
                }
                val searchResult = searcher.search(query)
                val searcherStats = searcher.getStats()
                ctx.html(FrontPage.createHtml(searcherStats, searchResult))
            } catch (e: Exception) {
                ctx.html(ResultPage.createHtml("ERROR: ${e.message ?: "INTERNAL ERROR"}"))
            }
        }

        server.post(ADD_PATH) { ctx ->
            try {
                val urlString = ctx.formParam(ADD_FORM_PARAM)
                if (urlString == null) {
                    ctx.html(ResultPage.createHtml("ERROR: $ADD_FORM_PARAM parameter was not found"))
                    return@post
                }
                val uri = URL(urlString).toURI() // throws if url is not formatted according to RFC2396
                val startIndexingResult = searcher.startIndexing(uri)
                when (startIndexingResult) {
                    is Started -> ctx.html(ResultPage.createHtml("Page was successfully added to index"))
                    is Refused -> ctx.html(
                        ResultPage.createHtml("ERROR: ${startIndexingResult.reason ?: "INTERNAL ERROR"}")
                    )
                }
            } catch (e: Exception) {
                ctx.html(ResultPage.createHtml("ERROR: ${e.message ?: "INTERNAL ERROR"}"))
            }
        }
    }

    companion object {
        const val BASE_PATH = "/"
        const val SEARCH_PATH = "/search"
        const val ADD_PATH = "/add"

        const val SEARCH_QUERY_PARAM = "text"
        const val ADD_FORM_PARAM = "url"
    }
}