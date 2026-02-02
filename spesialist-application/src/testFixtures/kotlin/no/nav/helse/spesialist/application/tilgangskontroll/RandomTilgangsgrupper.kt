package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun randomTilgangsgruppeUuider(): TilgangsgruppeUuider = TilgangsgruppeUuider(
    egenAnsattGruppeUuid = UUID.randomUUID(),
    kode7GruppeUuid = UUID.randomUUID(),
)
