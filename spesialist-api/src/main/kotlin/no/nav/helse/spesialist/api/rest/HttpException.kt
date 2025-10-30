package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode

abstract class HttpException(
    val statusCode: HttpStatusCode,
    val title: String?,
    val detail: String?,
) : Exception(
        buildString {
            append("HTTP ")
            append(statusCode.value)
            append(" - ")
            append(title)
            if (detail != null) {
                append(" - ")
                append(detail)
            }
        },
    )

class HttpUnauthorized(
    title: String? = null,
    detail: String? = null,
) : HttpException(HttpStatusCode.Unauthorized, title, detail)

class HttpForbidden(
    title: String? = null,
    detail: String? = null,
) : HttpException(HttpStatusCode.Forbidden, title, detail)

class HttpNotFound(
    title: String? = null,
    detail: String? = null,
) : HttpException(HttpStatusCode.NotFound, title, detail)

class HttpBadRequest(
    title: String? = null,
    detail: String? = null,
) : HttpException(HttpStatusCode.BadRequest, title, detail)

class HttpConflict(
    title: String? = null,
    detail: String? = null,
) : HttpException(HttpStatusCode.Conflict, title, detail)
