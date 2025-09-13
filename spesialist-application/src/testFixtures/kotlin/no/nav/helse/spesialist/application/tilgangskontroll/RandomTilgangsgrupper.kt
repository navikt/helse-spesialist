package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun randomTilgangsgruppeUuider(): TilgangsgruppeUuider = TilgangsgruppeUuider(
    beslutterGruppeUuid = UUID.randomUUID(),
    egenAnsattGruppeUuid = UUID.randomUUID(),
    kode7GruppeUuid = UUID.randomUUID(),
    stikkprøveGruppeUuid = UUID.randomUUID(),
    tbdGruppeUuid = UUID.randomUUID(),
)
