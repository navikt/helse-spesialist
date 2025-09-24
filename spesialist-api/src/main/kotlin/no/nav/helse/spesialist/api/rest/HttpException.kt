package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode

abstract class HttpException(
    val statusCode: HttpStatusCode,
) : Exception()

class HttpUnauthorized : HttpException(HttpStatusCode.Unauthorized)

class HttpForbidden : HttpException(HttpStatusCode.Forbidden)

class HttpNotFound : HttpException(HttpStatusCode.NotFound)
