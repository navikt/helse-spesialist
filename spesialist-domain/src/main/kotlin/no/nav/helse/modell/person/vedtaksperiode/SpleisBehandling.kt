package no.nav.helse.modell.person.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

data class SpleisBehandling(
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val yrkesaktivitet: Yrkesaktivitet? = null,
) {
    data class Yrkesaktivitet(
        val yrkesaktivitetstype: String,
        val organisasjonsnummer: String? = null,
    )
}
