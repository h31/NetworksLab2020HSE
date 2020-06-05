package ru.hse.lyubortk.websearch.http.implementation

import ru.hse.lyubortk.websearch.http.EndpointBinder
import ru.hse.lyubortk.websearch.http.RequestContext
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

class RequestProcessor : EndpointBinder {
    private val getHandlers: MutableMap<String, (RequestContext) -> Unit> = ConcurrentHashMap()
    private val postHandlers: MutableMap<String, (RequestContext) -> Unit> = ConcurrentHashMap()

    fun processRequest(httpRequest: HttpRequest): Pair<HttpResponse, ConnectionAction> {
        // RFC states that requests without host header should be rejected
        if (httpRequest.headers.keys.none { it.equals(HOST_HEADER, true) }) {
            return Pair(BAD_REQUEST, ConnectionAction.CLOSE)
        }

        val uri = try {
            URI(httpRequest.requestTarget)
        } catch (e: Exception) {
            return Pair(BAD_REQUEST, ConnectionAction.CLOSE)
        }

        val path = uri.path ?: return Pair(BAD_REQUEST, ConnectionAction.CLOSE)
        val queryParameters: Map<String, String> = uri.query?.parseParams() ?: emptyMap()
        val formParameters: Map<String, String> =
            httpRequest.body?.let { String(it.toByteArray()) }?.parseParams() ?: emptyMap()
        val context = RequestContextImpl(queryParameters, formParameters)

        val handler = when (httpRequest.method) {
            GET, HEAD -> getHandlers[path] ?: return Pair(NOT_FOUND, ConnectionAction.CLOSE)
            POST -> postHandlers[path] ?: return Pair(NOT_FOUND, ConnectionAction.CLOSE)
            else -> return Pair(METHOD_NOT_ALLOWED, ConnectionAction.CLOSE)
        }

        try {
            handler(context)
        } catch (e: Exception) {
            return Pair(INTERNAL_SERVER_ERROR, ConnectionAction.CLOSE)
        }
        val response = if (httpRequest.method == HEAD) {
            context.convertToResponse().copy(body = null)
        } else {
            context.convertToResponse()
        }

        return Pair(response, ConnectionAction.CLOSE)
    }

    fun getNotImplementedResponse() = NOT_IMPLEMENTED_RESPONSE

    fun getBadRequestResponse() = BAD_REQUEST

    // also binds to HEAD requests
    override fun get(path: String, routeHandler: (RequestContext) -> Unit) {
        getHandlers[path] = routeHandler
    }

    override fun post(path: String, routeHandler: (RequestContext) -> Unit) {
        postHandlers[path] = routeHandler
    }

    companion object {

        private class RequestContextImpl(
            private val queryParameters: Map<String, String>,
            private val formParameters: Map<String, String>
        ) : RequestContext {

            private var contentType: String? = null
            private var body: List<Byte>? = null

            override fun html(html: String) {
                contentType = "text/html"
                body = html.toByteArray().toList()
            }

            override fun queryParam(key: String): String? = queryParameters[key]

            override fun formParam(key: String): String? = formParameters[key]

            fun convertToResponse(): HttpResponse {
                val headers: MutableMap<String, List<String>> = mutableMapOf(
                    CONNECTION_HEADER to listOf(CONNECTION_CLOSE_VALUE),
                    CONTENT_LENGTH_HEADER to listOf(body?.size?.toString() ?: "0")
                )
                val type = contentType
                if (type != null) {
                    headers[CONTENT_TYPE_HEADER] = listOf(type)
                }
                return HttpResponse(
                    HTTP_VERSION,
                    200,
                    "OK",
                    headers,
                    body
                )
            }
        }

        private fun String.parseParams(): Map<String, String> = this
            .split("&")
            .map { parameter ->
                val index = parameter.indexOf('=')
                if (index == -1) {
                    URLDecoder.decode(parameter, "UTF-8") to ""
                }
                URLDecoder.decode(parameter.substring(0, index), "UTF-8") to
                        URLDecoder.decode(parameter.substring(index + 1), "UTF-8")
            }
            .toMap()

        private const val HTTP_VERSION = "HTTP/1.1"

        const val GET = "GET"
        const val POST = "POST"
        const val HEAD = "HEAD"

        const val HOST_HEADER = "Host"
        const val CONNECTION_HEADER = "Connection"
        const val CONTENT_LENGTH_HEADER = "Content-Length"
        const val CONTENT_TYPE_HEADER = "Content-Type"
        const val CONNECTION_CLOSE_VALUE = "close"

        private fun createBasicResponse(statusCode: Int, reasonPhrase: String) = HttpResponse(
            HTTP_VERSION,
            statusCode,
            reasonPhrase,
            mapOf(CONNECTION_HEADER to listOf(CONNECTION_CLOSE_VALUE)),
            null
        )

        private val BAD_REQUEST = createBasicResponse(
            400,
            "Bad Request"
        )

        private val NOT_FOUND = createBasicResponse(
            404,
            "Not Found"
        )

        private val METHOD_NOT_ALLOWED = createBasicResponse(
            405,
            "Method Not Allowed"
        )

        private val INTERNAL_SERVER_ERROR = createBasicResponse(
            500,
            "Internal Server Error"
        )

        private val NOT_IMPLEMENTED_RESPONSE = createBasicResponse(
            501,
            "Not Implemented"
        )
    }
}

