package no.nav.helse

import java.util.UUID
import no.nav.helse.mediator.api.ApiTilgangskontroll

val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }
fun idForGruppe(gruppe: Gruppe) = Tilgangsgrupper(testEnv).gruppeId(gruppe).toString()

internal fun lagApiTilgangskontroll(tilgangsgrupper: Tilgangsgrupper, gruppemedlemskap: List<UUID>) =
    ApiTilgangskontroll { gruppe ->
        Gruppe.entries.associateWith { tilgangsgrupper.harTilgang(gruppemedlemskap, it) }[gruppe]!!
    }

internal fun medTilgangTil(vararg innloggetBrukersGrupper: Gruppe = emptyArray()) =
    lagApiTilgangskontroll(
        Tilgangsgrupper(testEnv),
        innloggetBrukersGrupper.map(::idForGruppe).map(UUID::fromString)
    )

internal fun utenNoenTilganger(vararg innloggetBrukersGrupper: String = emptyArray()) =
    lagApiTilgangskontroll(
        Tilgangsgrupper(testEnv),
        innloggetBrukersGrupper.map(UUID::fromString)
    )

