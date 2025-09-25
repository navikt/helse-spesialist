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
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.PersonTilgangskontroll
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID
import kotlin.reflect.KClass

class RestHandler(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val meldingPubliserer: MeldingPubliserer,
) {
    suspend fun <RESPONSE, URLPARAMETRE> håndterGet(
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
            call.respond(statusCode)
        }.onSuccess { result ->
            call.respond(result as Any)
        }
    }

    suspend inline fun <URLPARAMETRE, reified REQUESTBODY : Any, RESPONSE> håndterPost(
        call: RoutingCall,
        håndterer: PostHåndterer<URLPARAMETRE, REQUESTBODY, RESPONSE>,
        noinline parameterTolkning: (Parameters) -> URLPARAMETRE,
    ) {
        håndterPost(call, håndterer, parameterTolkning, REQUESTBODY::class)
    }

    suspend fun <REQUESTBODY : Any, RESPONSE, URLPARAMETRE> håndterPost(
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

    fun kontrollerTilgangTilPerson(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        feilSupplier: () -> HttpException,
    ) {
        if (!PersonTilgangskontroll.harTilgangTilPerson(
                tilgangsgrupper = tilgangsgrupper,
                fødselsnummer = fødselsnummer,
                egenAnsattDao = transaksjon.egenAnsattDao,
                personDao = transaksjon.personDao,
            )
        ) {
            logg.warn("Saksbehandler mangler nødvendig tilgang")
            sikkerlogg.warn("Saksbehandler ${saksbehandler.id().value} mangler nødvendig tilgang til fødselsnummer $fødselsnummer")
            throw feilSupplier()
        }
    }

    companion object {
        fun Parameters.getRequired(name: String): String = this[name] ?: throw HttpNotFound("Mangler parameter $name i URL'en")

        fun Parameters.getRequiredUUID(name: String): UUID {
            val string = getRequired(name)
            try {
                return UUID.fromString(string)
            } catch (_: IllegalArgumentException) {
                throw HttpNotFound("Parameter $name i URL'en er ikke en UUID")
            }
        }
    }
}
