package ru.hse.lyubortk.websearch.http

interface HttpServer {
    fun get(path: String, routeHandler: (RequestContext) -> Unit)
    fun post(path: String, routeHandler: (RequestContext) -> Unit)
}

interface RequestContext {
    fun html(html: String)
    fun queryParam(key: String): String?
    fun formParam(key: String): String?
}