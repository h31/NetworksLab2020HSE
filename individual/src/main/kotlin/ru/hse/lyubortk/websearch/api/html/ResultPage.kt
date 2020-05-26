package ru.hse.lyubortk.websearch.api.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML

object ResultPage {
    fun createHtml(result: String) = createHTML().html {
        head {
            meta {
                charset = "UTF-8"
            }
        }
        body {
            h2 { +result }
            a("/") {
                +"back"
            }
        }
    }
}