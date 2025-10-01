package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
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
import kotlin.reflect.KClass

class RestDelegator(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val meldingPubliserer: MeldingPubliserer,
) {
    suspend fun <URLPARAMETERS : Any, RESPONSEBODY> utførGet(
        call: RoutingCall,
        håndterer: GetHåndterer<URLPARAMETERS, RESPONSEBODY>,
    ) {
        wrapOgDeleger(
            call,
            håndterer::extractParametre,
        ) { urlParametre, saksbehandler, tilgangsgrupper, transaksjon, _ ->
            håndterer.håndter(
                urlParametre = urlParametre,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        }
    }

    suspend fun <URLPARAMETERS : Any, REQUESTBODY : Any, RESPONSEBODY> utførPost(
        call: RoutingCall,
        håndterer: PostHåndterer<URLPARAMETERS, REQUESTBODY, RESPONSEBODY>,
    ) {
        @Suppress("UNCHECKED_CAST")
        val requestType = håndterer.requestBodyType.classifier as KClass<REQUESTBODY>
        val request: REQUESTBODY = call.receive(requestType)
        wrapOgDeleger(
            call = call,
            parameterTolkning = håndterer::extractParametre,
            håndterer = { urlParametre, saksbehandler, tilgangsgrupper, transaksjon, meldingsKø ->
                håndterer.håndter(
                    urlParametre = urlParametre,
                    requestBody = request,
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    transaksjon = transaksjon,
                    meldingsKø = meldingsKø,
                )
            },
        )
    }

    private suspend fun <RESPONSEBODY, URLPARAMETRE> wrapOgDeleger(
        call: RoutingCall,
        parameterTolkning: (pathParameters: Parameters, queryParameters: Parameters) -> URLPARAMETRE,
        håndterer: (URLPARAMETRE, Saksbehandler, Set<Tilgangsgruppe>, SessionContext, KøetMeldingPubliserer) -> RestResponse<RESPONSEBODY>,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            val urlParametre = parameterTolkning.invoke(call.parameters, call.request.queryParameters)
            val meldingsKø = KøetMeldingPubliserer(meldingPubliserer)
            sessionFactory
                .transactionalSessionScope { transaksjon ->
                    håndterer.invoke(
                        urlParametre,
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
