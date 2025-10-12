package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.gruppeUuider
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.tilSaksbehandler
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class RestAdapter(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val meldingPubliserer: MeldingPubliserer,
) {
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
        wrapOgDeleger(call) { saksbehandler, tilgangsgrupper, transaksjon, meldingsKø ->
            behandler.behandle(
                resource = resource,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                meldingsKø = meldingsKø,
            )
        }
    }

    suspend inline fun <RESOURCE, reified REQUEST, RESPONSE> behandle(
        resource: RESOURCE,
        call: RoutingCall,
        behandler: RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE>,
    ) {
        val request: REQUEST = call.receive()
        wrapOgDeleger(call) { saksbehandler, tilgangsgrupper, transaksjon, meldingsKø ->
            behandler.behandle(
                resource = resource,
                request = request,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
                meldingsKø = meldingsKø,
            )
        }
    }

    suspend fun <RESPONSE> wrapOgDeleger(
        call: RoutingCall,
        behandler: (Saksbehandler, Set<Tilgangsgruppe>, SessionContext, KøetMeldingPubliserer) -> RestResponse<RESPONSE>,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            val meldingsKø = KøetMeldingPubliserer(meldingPubliserer)
            sessionFactory
                .transactionalSessionScope { transaksjon ->
                    behandler.invoke(
                        saksbehandler,
                        tilgangsgrupper,
                        transaksjon,
                        meldingsKø,
                    )
                }.also { meldingsKø.flush() }
        }.onFailure { cause ->
            val statusCode = (cause as? HttpException)?.statusCode ?: HttpStatusCode.InternalServerError
            loggThrowable("Returnerer HTTP ${statusCode.value}", cause)
            call.respond(statusCode, "{ \"httpStatusCode\": ${statusCode.value} }")
        }.onSuccess { result ->
            call.response.status(result.statusCode)
            call.respond(result.body as Any)
        }
    }
}
