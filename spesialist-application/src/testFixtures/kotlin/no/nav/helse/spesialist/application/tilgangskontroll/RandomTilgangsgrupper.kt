package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

fun randomTilgangsgrupper(): Tilgangsgrupper = Tilgangsgrupper(
    kode7GruppeId = UUID.randomUUID(),
    beslutterGruppeId = UUID.randomUUID(),
    skjermedePersonerGruppeId = UUID.randomUUID(),
    stikkprøveGruppeId = UUID.randomUUID()
)
