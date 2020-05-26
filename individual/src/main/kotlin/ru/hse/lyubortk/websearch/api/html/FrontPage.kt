package ru.hse.lyubortk.websearch.api.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.net.URL

object FrontPage {
    fun createHtml(results: List<SearchResult>?): String {
        return createHTML().html {
            head {
                meta {
                    charset = "UTF-8"
                }
            }
            body {
                h2 { +"Simple search engine" }
                form {
                    action = "/add"
                    method = FormMethod.post
                    acceptCharset = "UTF-8"
                    input {
                        type = InputType.text
                        name = "url"
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
                    action = "/search"
                    method = FormMethod.get
                    acceptCharset = "UTF-8"
                    input {
                        type = InputType.text
                        name = "text"
                    }
                    br
                    input {
                        type = InputType.submit
                        value = "Search"
                    }
                }

                if (results != null) {
                    br
                    if (results.isNotEmpty()) {
                        h3 { +"Results:" }
                    } else {
                        h3 { +"Nothing was found" }
                    }

                    results.forEach { searchResult ->
                        div {
                            a(searchResult.url.toString()) {
                                +searchResult.name
                            }
                        }
                        br
                    }
                }
            }
        }
    }

    data class SearchResult(val url: URL, val name: String)
}