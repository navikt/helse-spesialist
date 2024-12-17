package no.nav.helse.modell.person.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

data class SpleisBehandling(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun erRelevantFor(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}
