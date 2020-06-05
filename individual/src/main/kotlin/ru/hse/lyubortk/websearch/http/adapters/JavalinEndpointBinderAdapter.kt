package ru.hse.lyubortk.websearch.http.adapters

import io.javalin.Javalin
import io.javalin.http.Context
import ru.hse.lyubortk.websearch.http.HttpServerEndpointBinder
import ru.hse.lyubortk.websearch.http.RequestContext

class JavalinEndpointBinderAdapter(private val javalin: Javalin) : HttpServerEndpointBinder {
    override fun get(path: String, routeHandler: (RequestContext) -> Unit) {
        javalin.get(path) { routeHandler(adaptJavalinContext(it)) }
    }

    override fun post(path: String, routeHandler: (RequestContext) -> Unit) {
        javalin.post(path) { routeHandler(adaptJavalinContext(it)) }
    }

    private fun adaptJavalinContext(javalinContext: Context): RequestContext {
        return object : RequestContext {
            override fun html(html: String) {
                javalinContext.html(html)
            }

            override fun queryParam(key: String): String? = javalinContext.queryParam(key)

            override fun formParam(key: String): String? = javalinContext.formParam(key)
        }
    }
}