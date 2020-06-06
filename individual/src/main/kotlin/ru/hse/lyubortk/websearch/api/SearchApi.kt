package ru.hse.lyubortk.websearch.api

import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.api.html.FrontPage
import ru.hse.lyubortk.websearch.api.html.ResultPage
import ru.hse.lyubortk.websearch.core.Searcher
import ru.hse.lyubortk.websearch.core.Searcher.Companion.StartIndexingResult.Refused
import ru.hse.lyubortk.websearch.core.Searcher.Companion.StartIndexingResult.Started
import ru.hse.lyubortk.websearch.http.HttpServer
import ru.hse.lyubortk.websearch.http.RequestContext
import java.net.URL

class SearchApi(server: HttpServer, searcher: Searcher) {
    private val log = LoggerFactory.getLogger(SearchApi::class.java)

    init {
        server.get(BASE_PATH, withErrorHandling(BASE_PATH) { ctx ->
            ctx.html(FrontPage.createHtml(searcher.getStats(), null))
        })

        server.get(SEARCH_PATH, withErrorHandling(SEARCH_PATH) { ctx ->
            val query = ctx.queryParam(SEARCH_QUERY_PARAM)
            if (query == null) {
                ctx.html(ResultPage.createHtml("ERROR: $SEARCH_QUERY_PARAM parameter was not found"))
                return@withErrorHandling
            }
            val searchResult = searcher.search(query)
            val searcherStats = searcher.getStats()
            ctx.html(FrontPage.createHtml(searcherStats, searchResult))
        })

        server.post(ADD_PATH, withErrorHandling(ADD_PATH) { ctx ->
            val urlString = ctx.formParam(ADD_FORM_PARAM)
            if (urlString == null) {
                ctx.html(ResultPage.createHtml("ERROR: $ADD_FORM_PARAM parameter was not found"))
                return@withErrorHandling
            }
            val uri = URL(urlString).toURI() // throws if url is not formatted according to RFC2396
            when (val startIndexingResult = searcher.startIndexing(uri)) {
                is Started -> ctx.html(ResultPage.createHtml("Page was successfully added to index"))
                is Refused -> ctx.html(
                    ResultPage.createHtml("ERROR: ${startIndexingResult.reason ?: "INTERNAL ERROR"}")
                )
            }
        })
    }

    private fun withErrorHandling(routeName: String, handler: (RequestContext) -> Unit): (RequestContext) -> Unit {
        return { ctx ->
            try {
                handler(ctx)
            } catch (e: Exception) {
                log.error("Exception in $routeName route", e)
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