package no.nav.helse.modell.saksbehandler.handlinger.dto

import java.time.LocalDate
import java.util.UUID

data class MinimumSykdomsgradDto(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdering: Boolean,
    val begrunnelse: String,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiverDto>,
    val initierendeVedtaksperiodeId: UUID,
) {
    data class MinimumSykdomsgradArbeidsgiverDto(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )
}
