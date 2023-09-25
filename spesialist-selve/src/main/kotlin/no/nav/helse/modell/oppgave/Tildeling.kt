package no.nav.helse.modell.oppgave

import java.util.UUID

data class Tildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
    val påVent: Boolean,
)