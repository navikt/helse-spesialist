package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode

abstract class HttpException(
    val statusCode: HttpStatusCode,
    message: String?,
) : Exception(message)

class HttpUnauthorized(
    message: String? = null,
) : HttpException(HttpStatusCode.Unauthorized, message)

class HttpForbidden(
    message: String? = null,
) : HttpException(HttpStatusCode.Forbidden, message)

class HttpNotFound(
    message: String? = null,
) : HttpException(HttpStatusCode.NotFound, message)

class HttpBadRequest(
    message: String? = null,
) : HttpException(HttpStatusCode.BadRequest, message)

class HttpConflict(
    message: String? = null,
) : HttpException(HttpStatusCode.Conflict, message)
