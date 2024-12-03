package no.nav.helse.spesialist.api.graphql.schema

import java.time.LocalDate
import java.util.UUID

sealed interface PaVentRequest {
    data class LeggPaVent(
        val oppgaveId: Long,
        val saksbehandlerOid: UUID,
        val frist: LocalDate,
        val skalTildeles: Boolean,
        val notatTekst: String?,
        val årsaker: List<PaVentArsak>,
    ) : PaVentRequest

    data class FjernPaVent(
        val oppgaveId: Long,
    ) : PaVentRequest

    data class FjernPaVentUtenHistorikkinnslag(
        val oppgaveId: Long,
    ) : PaVentRequest

    data class OppdaterPaVentFrist(
        val oppgaveId: Long,
        val saksbehandlerOid: UUID,
        val frist: LocalDate,
        val skalTildeles: Boolean,
        val notatTekst: String?,
        val årsaker: List<PaVentArsak>,
    ) : PaVentRequest

    data class PaVentArsak(
        val _key: String,
        val arsak: String,
    )
}
