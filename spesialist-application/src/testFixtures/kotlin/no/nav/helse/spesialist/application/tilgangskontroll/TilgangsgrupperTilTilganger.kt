package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun tilgangsgrupperTilTilganger(
    lesetilgang: List<UUID> = listOf(UUID.randomUUID()),
    skrivetilgang: List<UUID> = listOf(UUID.randomUUID()),
): TilgangsgrupperTilTilganger =
    TilgangsgrupperTilTilganger(
        lesetilgang = lesetilgang,
        skrivetilgang = skrivetilgang,
    )
