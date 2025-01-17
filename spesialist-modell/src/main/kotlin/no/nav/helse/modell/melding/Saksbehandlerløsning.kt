package no.nav.helse.modell.melding

import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

data class Saksbehandlerløsning(
    val godkjenningsbehovId: UUID,
    val oppgaveId: Long,
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerEpost: String,
    val godkjenttidspunkt: LocalDateTime,
    val saksbehandleroverstyringer: List<UUID>,
    val saksbehandler: Saksbehandler,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val beslutter: Saksbehandler?,
) : UtgåendeHendelse
