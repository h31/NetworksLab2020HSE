package ru.hse.lyubortk.websearch.api.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import ru.hse.lyubortk.websearch.api.RequestHandler.Companion.ADD_FORM_PARAM
import ru.hse.lyubortk.websearch.api.RequestHandler.Companion.ADD_PATH
import ru.hse.lyubortk.websearch.api.RequestHandler.Companion.SEARCH_PATH
import ru.hse.lyubortk.websearch.api.RequestHandler.Companion.SEARCH_QUERY_PARAM
import ru.hse.lyubortk.websearch.core.Searcher.Companion.SearchResult

object FrontPage {
    fun createHtml(searchResult: SearchResult?): String {
        return createHTML().html {
            head {
                meta {
                    charset = "UTF-8"
                }
            }
            body {
                h2 { +"Simple search engine" }
                form {
                    action = ADD_PATH
                    method = FormMethod.post
                    acceptCharset = "UTF-8"
                    input {
                        type = InputType.text
                        name = ADD_FORM_PARAM
                    }
                    br
                    input {
                        type = InputType.submit
                        value = "Add to index"
                    }
                }
                br
                br
                form {
                    action = SEARCH_PATH
                    method = FormMethod.get
                    acceptCharset = "UTF-8"
                    input {
                        type = InputType.text
                        name = SEARCH_QUERY_PARAM
                    }
                    br
                    input {
                        type = InputType.submit
                        value = "Search"
                    }
                }

                if (searchResult != null) {
                    br
                    val isExact = searchResult.totalResults.isExact
                    val numberOfPages = searchResult.totalResults.number
                    val query = searchResult.searchQuery
                    +"Found ${if (isExact) "" else "more than"} $numberOfPages pages for query \"$query\""
                    br
                    +"Showing top ${searchResult.topPages.size} results"
                    br

                    searchResult.topPages.forEach { page ->
                        br
                        div {
                            a(page.url.toString()) {
                                +page.title
                            }
                        }
                    }
                }
            }
        }
    }
}