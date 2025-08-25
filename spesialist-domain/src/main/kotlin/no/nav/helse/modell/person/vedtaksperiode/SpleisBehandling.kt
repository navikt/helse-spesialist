package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import java.time.LocalDate
import java.util.UUID

data class SpleisBehandling(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
) {
    fun erRelevantFor(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}
