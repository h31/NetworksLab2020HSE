package ru.hse.lyubortk.websearch.api.html

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h2
import kotlinx.html.html
import kotlinx.html.stream.createHTML

object AddedToIndexPage {
    val html = createHTML().html {
        body {
            h2 { +"Url successfully added to index" }
            a("/") {
                +"back"
            }
        }
    }
}