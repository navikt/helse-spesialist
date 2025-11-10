package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.resources.href
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.gruppeUuider
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.tilSaksbehandler
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.loggWarnThrowable
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class RestAdapter(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val meldingPubliserer: MeldingPubliserer,
    private val resourcesFormat: ResourcesFormat,
    private val versjonAvKode: String,
) {
    private val problemObjectMapper = objectMapper.copy().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    suspend inline fun <RESOURCE, RESPONSE, ERROR : ApiErrorCode> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: GetBehandler<RESOURCE, RESPONSE, ERROR>,
    ) {
        wrapOgDeleger(call) { saksbehandler, tilgangsgrupper, transaksjon, _ ->
            behandler.behandle(
                resource = resource,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        }
    }

    suspend inline fun <RESOURCE, RESPONSE, ERROR : ApiErrorCode> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: DeleteBehandler<RESOURCE, RESPONSE, ERROR>,
    ) {
        wrapOgDeleger(call) { saksbehandler, tilgangsgrupper, transaksjon, outbox ->
            behandler.behandle(
                resource = resource,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                outbox = outbox,
            )
        }
    }

    suspend inline fun <RESOURCE, reified REQUEST, RESPONSE, ERROR : ApiErrorCode> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>,
    ) {
        val request: REQUEST = call.receive()
        wrapOgDeleger(call) { saksbehandler, tilgangsgrupper, transaksjon, outbox ->
            behandler.behandle(
                resource = resource,
                request = request,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                outbox = outbox,
            )
        }
    }

    suspend fun <RESPONSE, ERROR : ApiErrorCode> wrapOgDeleger(
        call: RoutingCall,
        behandler: (Saksbehandler, Set<Tilgangsgruppe>, SessionContext, Outbox) -> RestResponse<RESPONSE, ERROR>,
    ) {
        val jwt =
            (
                call.principal<JWTPrincipal>() ?: run {
                    call.respondWithProblem(genericProblemDetails(HttpStatusCode.Unauthorized))
                    return@wrapOgDeleger
                }
            ).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            val outbox = Outbox(versjonAvKode)
            sessionFactory
                .transactionalSessionScope { transaksjon ->
                    val response =
                        behandler.invoke(
                            saksbehandler,
                            tilgangsgrupper,
                            transaksjon,
                            outbox,
                        )

                    if (response is RestResponse.Error) {
                        throw WrappedApiHttpProblemDetailsException(
                            ApiHttpProblemDetails(
                                status = response.errorCode.statusCode.value,
                                title = response.errorCode.title,
                                detail = response.detail,
                                code = response.errorCode,
                            ),
                        )
                    }
                    response as RestResponse.Success
                }.also { outbox.sendAlle(meldingPubliserer) }
        }.onFailure { cause ->
            val problemDetails =
                if (cause is WrappedApiHttpProblemDetailsException) {
                    cause.problemDetails
                } else {
                    genericProblemDetails<ERROR>(HttpStatusCode.InternalServerError)
                }
            val statusCode = problemDetails.status
            val title = problemDetails.title
            val loggmelding =
                buildString {
                    append("Returnerer HTTP $statusCode - $title")
                    if (cause !is WrappedApiHttpProblemDetailsException) {
                        append(" (pga. ${cause::class.simpleName})")
                    }
                }
            if (statusCode in 400..<500) {
                loggWarnThrowable(loggmelding, cause)
            } else {
                loggThrowable(loggmelding, cause)
            }
            call.respondWithProblem(problemDetails)
        }.onSuccess { result ->
            when (result) {
                is RestResponse.NoContent<*> -> call.respond(HttpStatusCode.NoContent)
                is RestResponse.OK<*, *> -> call.respond(HttpStatusCode.OK, result.body as Any)
                is RestResponse.Created<*> -> {
                    call.response.headers.append(HttpHeaders.Location, href(resourcesFormat, result.location))
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }

    private suspend fun RoutingCall.respondWithProblem(problemDetails: ApiHttpProblemDetails<out ApiErrorCode>) {
        respondText(
            problemObjectMapper.writeValueAsString(problemDetails),
            ContentType.Application.ProblemJson,
            HttpStatusCode.fromValue(problemDetails.status),
        )
    }

    private fun <T : ApiErrorCode> genericProblemDetails(internalServerError: HttpStatusCode): ApiHttpProblemDetails<T> =
        ApiHttpProblemDetails(
            status = internalServerError.value,
            title = internalServerError.description,
            code = null,
        )
}

private class WrappedApiHttpProblemDetailsException(
    val problemDetails: ApiHttpProblemDetails<*>,
) : Exception()
