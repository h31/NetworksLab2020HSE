package ru.hse.lyubortk.websearch.api.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import ru.hse.lyubortk.websearch.api.RequestHandler.Companion.BASE_PATH

object ResultPage {
    fun createHtml(result: String) = createHTML().html {
        head {
            meta {
                charset = "UTF-8"
            }
        }
        body {
            h2 { +result }
            a(BASE_PATH) {
                +"back"
            }
        }
    }
}