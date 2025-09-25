package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.gruppeUuider
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.tilSaksbehandler
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import kotlin.reflect.KClass

class RestDelegator(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val meldingPubliserer: MeldingPubliserer,
) {
    suspend fun <RESPONSE, URLPARAMETRE> utførGet(
        call: RoutingCall,
        håndterer: GetHåndterer<URLPARAMETRE, RESPONSE>,
        parameterTolkning: (Parameters) -> URLPARAMETRE,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            sessionFactory.transactionalSessionScope { tx ->
                håndterer.håndter(
                    urlParametre = parameterTolkning.invoke(call.parameters),
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    transaksjon = tx,
                )
            }
        }.onFailure { cause ->
            val statusCode = (cause as? HttpException)?.statusCode ?: HttpStatusCode.InternalServerError
            loggThrowable("Returnerer HTTP ${statusCode.value}", cause)
            // GraphQL-biblioteket vi fortsatt bruker i Speil for å snakke med REST, liker dårlig å ikke få noe
            // body i svaret. Vi legger det derfor på her så lenge vi har GraphQL der.
            call.respond(statusCode, false)
        }.onSuccess { result ->
            if (result is HttpStatusCode) {
                // GraphQL-biblioteket vi fortsatt bruker i Speil for å snakke med REST, liker dårlig å ikke få noe
                // body i svaret. Vi legger det derfor på her så lenge vi har GraphQL der.
                call.respond(result, true)
            } else {
                call.respond(result as Any)
            }
        }
    }

    suspend inline fun <URLPARAMETRE, reified REQUESTBODY : Any, RESPONSE> utførPost(
        call: RoutingCall,
        håndterer: PostHåndterer<URLPARAMETRE, REQUESTBODY, RESPONSE>,
        noinline parameterTolkning: (Parameters) -> URLPARAMETRE,
    ) {
        utførPost(call, håndterer, parameterTolkning, REQUESTBODY::class)
    }

    suspend fun <REQUESTBODY : Any, RESPONSE, URLPARAMETRE> utførPost(
        call: RoutingCall,
        håndterer: PostHåndterer<URLPARAMETRE, REQUESTBODY, RESPONSE>,
        parameterTolkning: (Parameters) -> URLPARAMETRE,
        requestType: KClass<REQUESTBODY>,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        val meldingsKø = KøetMeldingPubliserer(meldingPubliserer)
        val request: REQUESTBODY = call.receive(type = requestType)
        runCatching {
            sessionFactory
                .transactionalSessionScope { tx ->
                    håndterer.håndter(
                        urlParametre = parameterTolkning.invoke(call.parameters),
                        requestBody = request,
                        saksbehandler = saksbehandler,
                        tilgangsgrupper = tilgangsgrupper,
                        transaksjon = tx,
                        meldingsKø = meldingsKø,
                    )
                }.also { meldingsKø.flush() }
        }.onFailure { cause ->
            val statusCode = (cause as? HttpException)?.statusCode ?: HttpStatusCode.InternalServerError
            loggThrowable("Returnerer HTTP ${statusCode.value}", cause)
            call.respond(statusCode)
        }.onSuccess { result ->
            call.respond(result as Any)
        }
    }
}
