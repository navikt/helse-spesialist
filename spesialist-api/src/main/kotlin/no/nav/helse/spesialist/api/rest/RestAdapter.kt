package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
) {
    private val problemObjectMapper = objectMapper.copy().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    suspend inline fun <RESOURCE, RESPONSE> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: GetBehandler<RESOURCE, RESPONSE>,
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

    suspend inline fun <RESOURCE, RESPONSE> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: DeleteBehandler<RESOURCE, RESPONSE>,
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

    suspend inline fun <RESOURCE, reified REQUEST, RESPONSE> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE>,
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

    suspend fun <RESPONSE> wrapOgDeleger(
        call: RoutingCall,
        behandler: (Saksbehandler, Set<Tilgangsgruppe>, SessionContext, Outbox) -> RestResponse<RESPONSE>,
    ) {
        val jwt =
            (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized(title = "Token mangler i forespørsel")).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            val outbox = Outbox()
            sessionFactory
                .transactionalSessionScope { transaksjon ->
                    behandler.invoke(
                        saksbehandler,
                        tilgangsgrupper,
                        transaksjon,
                        outbox,
                    )
                }.also { outbox.sendAlle(meldingPubliserer) }
        }.onFailure { cause ->
            val statusCode = (cause as? HttpException)?.statusCode ?: HttpStatusCode.InternalServerError
            val title = (cause as? HttpException)?.title ?: statusCode.description
            val detail = (cause as? HttpException)?.detail
            if (statusCode.value in 400..<500) {
                loggWarnThrowable("Returnerer HTTP ${statusCode.value} - $title", cause)
            } else {
                loggThrowable("Returnerer HTTP ${statusCode.value} - $title", cause)
            }
            call.respondText(
                problemObjectMapper.writeValueAsString(
                    HttpProblemDetails(
                        title = title,
                        status = statusCode.value,
                        detail = detail,
                    ),
                ),
                ContentType.Application.ProblemJson,
                statusCode,
            )
        }.onSuccess { result ->
            call.response.status(result.statusCode)
            call.respond(result.body as Any)
        }
    }
}

private data class HttpProblemDetails(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String?,
)
