package no.nav.helse.spesialist.api.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID

sealed interface PåVentRequest {
    data class LeggPåVent(
        val oppgaveId: Long,
        val saksbehandlerOid: UUID,
        val frist: LocalDate,
        val skalTildeles: Boolean,
        val begrunnelse: String?,
        val notatTekst: String,
    ) : PåVentRequest

    data class FjernPåVent(
        val oppgaveId: Long,
    ) : PåVentRequest

    data class FjernPåVentUtenHistorikkinnslag(
        val oppgaveId: Long,
    ) : PåVentRequest
}
