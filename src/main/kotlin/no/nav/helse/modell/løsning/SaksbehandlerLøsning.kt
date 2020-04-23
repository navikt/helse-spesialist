package no.nav.helse.modell.løsning

import java.time.LocalDateTime

internal class SaksbehandlerLøsning(
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val oid: String,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime
)
