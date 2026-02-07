package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.auth.SaksbehandlerPrincipal
import no.nav.helse.spesialist.api.coMedMdcOgAttribute
import no.nav.helse.spesialist.api.mdcMapAttribute
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.loggWarnThrowable
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.slf4j.MDC

class RestAdapter(
    private val sessionFactory: SessionFactory,
    private val meldingPubliserer: MeldingPubliserer,
    private val versjonAvKode: String,
) {
    private val problemObjectMapper = objectMapper.copy().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    suspend inline fun <RESOURCE, RESPONSE, ERROR : ApiErrorCode> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>,
    ) {
        wrapOgDeleger(
            call,
            behandler.påkrevdeBrukerroller,
            behandler.påkrevdTilgang,
        ) { kallKontekst -> behandler.behandle(resource, kallKontekst) }
    }

    suspend inline fun <RESOURCE, reified REQUEST, RESPONSE, ERROR : ApiErrorCode> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>,
    ) {
        val request: REQUEST = call.receive()
        wrapOgDeleger(call, behandler.påkrevdeBrukerroller, behandler.påkrevdTilgang) { kallKontekst ->
            behandler.behandle(
                resource,
                request,
                kallKontekst,
            )
        }
    }

    suspend fun <RESPONSE, ERROR : ApiErrorCode> wrapOgDeleger(
        call: RoutingCall,
        påkrevdeBrukerroller: Set<Brukerrolle>,
        påkrevdTilgang: Tilgang,
        behandler: (KallKontekst) -> RestResponse<RESPONSE, ERROR>,
    ) {
        withSaksbehandlerIdentMdc(call) {
            val principal =
                call.principal<SaksbehandlerPrincipal>() ?: run {
                    call.respondWithProblem(genericProblemDetails(HttpStatusCode.Unauthorized))
                    return@withSaksbehandlerIdentMdc
                }

            if (påkrevdTilgang !in principal.tilganger) {
                call.respondWithProblem(genericProblemDetails(HttpStatusCode.Forbidden))
                return@withSaksbehandlerIdentMdc
            }

            val harPåkrevdeBrukerroller =
                principal.brukerroller.any { it in påkrevdeBrukerroller } || påkrevdeBrukerroller.isEmpty()
            if (!harPåkrevdeBrukerroller) {
                call.respondWithProblem(genericProblemDetails(HttpStatusCode.Forbidden))
                return@withSaksbehandlerIdentMdc
            }

            sessionFactory.transactionalSessionScope {
                it.saksbehandlerRepository.lagre(saksbehandler = principal.saksbehandler)
            }

            val result =
                runCatching {
                    val outbox = Outbox(versjonAvKode)
                    sessionFactory
                        .transactionalSessionScope { transaksjon ->
                            val kallKontekst =
                                KallKontekst(
                                    saksbehandler = principal.saksbehandler,
                                    brukerroller = principal.brukerroller,
                                    tilganger = principal.tilganger,
                                    transaksjon = transaksjon,
                                    outbox = outbox,
                                    ktorCall = call,
                                )
                            val response = behandler.invoke(kallKontekst)

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
                        }.also {
                            coMedMdcFraKall(call) {
                                outbox.sendAlle(meldingPubliserer)
                            }
                        }
                }

            coMedMdcFraKall(call) {
                result
                    .onFailure { cause ->
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
                            loggThrowable(
                                message = loggmelding,
                                teamLogsDetails =
                                    call
                                        .receive<String>()
                                        .takeUnless(String::isBlank)
                                        ?.let { "Request body: $it" }
                                        .orEmpty(),
                                throwable = cause,
                            )
                        }
                        call.respondWithProblem(problemDetails)
                    }.onSuccess { result ->
                        when (result) {
                            is RestResponse.NoContent<*> -> call.respond(HttpStatusCode.NoContent)
                            is RestResponse.OK<*, *> -> call.respond(HttpStatusCode.OK, result.body as Any)
                            is RestResponse.Created<*, *> -> call.respond(HttpStatusCode.Created, result.body as Any)
                        }
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

private suspend fun <T> coMedMdcFraKall(
    call: RoutingCall,
    block: suspend () -> T,
) {
    val mdcFromAttributes =
        MdcKey.entries
            .filterNot { it in setOf(MdcKey.REQUEST_METHOD, MdcKey.REQUEST_URI) }
            .mapNotNull { mdcKey ->
                call.mdcMapAttribute[mdcKey]?.let { mdcKey.value to it }
            }
    return withContext(MDCContext(MDC.getCopyOfContextMap().orEmpty() + mdcFromAttributes)) { block() }
}

private class WrappedApiHttpProblemDetailsException(
    val problemDetails: ApiHttpProblemDetails<*>,
) : Exception()

suspend fun <T> withSaksbehandlerIdentMdc(
    call: RoutingCall,
    block: suspend () -> T,
): T {
    val saksbehandlerIdent =
        call
            .principal<SaksbehandlerPrincipal>()
            ?.saksbehandler
            ?.ident
            ?.value ?: return block()
    return call.coMedMdcOgAttribute(MdcKey.SAKSBEHANDLER_IDENT to saksbehandlerIdent) {
        block()
    }
}
