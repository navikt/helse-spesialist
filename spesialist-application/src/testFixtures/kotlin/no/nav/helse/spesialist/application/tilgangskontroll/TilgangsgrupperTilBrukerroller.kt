package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun tilgangsgrupperTilBrukerroller(
    næringsdrivendeBeta: List<UUID> = listOf(UUID.randomUUID()),
    beslutter: List<UUID> = listOf(UUID.randomUUID()),
    egenAnsatt: List<UUID> = listOf(UUID.randomUUID()),
    kode7: List<UUID> = listOf(UUID.randomUUID()),
    stikkprøve: List<UUID> = listOf(UUID.randomUUID()),
    utvikler: List<UUID> = listOf(UUID.randomUUID()),
): TilgangsgrupperTilBrukerroller =
    TilgangsgrupperTilBrukerroller(
        næringsdrivendeBeta = næringsdrivendeBeta,
        beslutter = beslutter,
        egenAnsatt = egenAnsatt,
        kode7 = kode7,
        stikkprøve = stikkprøve,
        utvikler = utvikler,
    )
