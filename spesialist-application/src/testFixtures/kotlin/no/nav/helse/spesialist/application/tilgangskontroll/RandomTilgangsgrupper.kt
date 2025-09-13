package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun randomTilgangsgruppeUuider(): TilgangsgruppeUuider = TilgangsgruppeUuider(
    kode7GruppeUuid = UUID.randomUUID(),
    beslutterGruppeUuid = UUID.randomUUID(),
    skjermedePersonerGruppeUuid = UUID.randomUUID(),
    stikkprøveGruppeUuid = UUID.randomUUID(),
    tbdGruppeUuid = UUID.randomUUID(),
)
