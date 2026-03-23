package no.nav.helse.modell.melding

import java.time.LocalDateTime
import java.util.UUID

data class Godkjenningsbehovløsning(
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val godkjenttidspunkt: LocalDateTime,
    val automatiskBehandling: Boolean,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val saksbehandleroverstyringer: List<UUID>,
    val json: String,
) : UtgåendeHendelse
