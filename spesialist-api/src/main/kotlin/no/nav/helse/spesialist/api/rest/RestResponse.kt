package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode

class RestResponse<T>(
    val statusCode: HttpStatusCode,
    val body: T,
) {
    companion object {
        fun <T> created(body: T) = RestResponse(statusCode = HttpStatusCode.Created, body = body)

        fun <T> ok(body: T) = RestResponse(statusCode = HttpStatusCode.OK, body = body)

        fun noContent() = RestResponse(statusCode = HttpStatusCode.NoContent, body = Unit)
    }
}
