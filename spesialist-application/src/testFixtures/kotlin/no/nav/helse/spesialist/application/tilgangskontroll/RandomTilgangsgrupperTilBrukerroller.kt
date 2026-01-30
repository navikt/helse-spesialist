package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun randomTilgangsgrupperTilBrukerroller(): TilgangsgrupperTilBrukerroller =
    TilgangsgrupperTilBrukerroller(
        næringsdrivendeBeta = listOf(UUID.randomUUID()),
        beslutter = listOf(UUID.randomUUID()),
        egenAnsatt = listOf(UUID.randomUUID()),
        kode7 = listOf(UUID.randomUUID()),
        stikkprøve = listOf(UUID.randomUUID(), UUID.randomUUID())
    )