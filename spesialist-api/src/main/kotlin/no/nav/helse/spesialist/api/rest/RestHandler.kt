package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.gruppeUuider
import no.nav.helse.spesialist.api.graphql.ContextFactory.Companion.tilSaksbehandler
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.PersonTilgangskontroll
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.KClass

class RestHandler(
    private val sessionFactory: SessionFactory,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
) {
    suspend fun <RESPONSE> handleGet(
        call: RoutingCall,
        callback: (
            parametre: Parameters,
            saksbehandler: Saksbehandler,
            tilgangsgrupper: Set<Tilgangsgruppe>,
            transaksjon: SessionContext,
        ) -> RESPONSE,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        runCatching {
            sessionFactory.transactionalSessionScope { tx ->
                callback.invoke(
                    call.parameters,
                    saksbehandler,
                    tilgangsgrupper,
                    tx,
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

    suspend fun <REQUEST : Any, RESPONSE> handlePost(
        call: RoutingCall,
        requestType: KClass<REQUEST>,
        callback: (
            parametre: Parameters,
            request: REQUEST,
            saksbehandler: Saksbehandler,
            tilgangsgrupper: Set<Tilgangsgruppe>,
            transaksjon: SessionContext,
        ) -> RESPONSE,
    ) {
        val jwt = (call.principal<JWTPrincipal>() ?: throw HttpUnauthorized()).payload

        val saksbehandler = jwt.tilSaksbehandler()
        val tilgangsgrupper = tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider())

        sessionFactory.transactionalSessionScope {
            it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler)
        }

        val request: REQUEST = call.receive(requestType)

        runCatching {
            sessionFactory.transactionalSessionScope { tx ->
                callback.invoke(
                    call.parameters,
                    request,
                    saksbehandler,
                    tilgangsgrupper,
                    tx,
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
            sikkerlogg.warn("Saksbehandler mangler nødvendig tilgang til fødselsnummer $fødselsnummer")
            throw feilSupplier()
        }
    }

    companion object {
        fun Parameters.getRequired(name: String): String = this[name] ?: throw HttpNotFound()
    }
}
