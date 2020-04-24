package no.nav.helse.modell.løsning

import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerLøsning(
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val oid: UUID,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime
)
