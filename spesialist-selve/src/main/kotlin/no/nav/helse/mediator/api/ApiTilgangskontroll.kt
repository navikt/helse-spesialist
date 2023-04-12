package no.nav.helse.mediator.api

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangsgrupper

// Støttekode for å kunne gjøre tilgangskontroll fra API-ene

internal fun PipelineContext<Unit, ApplicationCall>.gruppemedlemskap(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}

private fun PipelineContext<Unit, ApplicationCall>.relevanteGrupper(tilgangsgrupper: Tilgangsgrupper): Map<Gruppe, Boolean> {
    val gruppemedlemskap = gruppemedlemskap()
    return Gruppe.values().associateWith { tilgangsgrupper.harTilgang(gruppemedlemskap, it) }
}

internal fun PipelineContext<Unit, ApplicationCall>.tilganger(tilgangsgrupper: Tilgangsgrupper) =
    ApiTilgangskontroll { gruppe -> relevanteGrupper(tilgangsgrupper)[gruppe]!! }

internal fun interface ApiTilgangskontroll {
    fun harTilgangTil(gruppe: Gruppe): Boolean
}
