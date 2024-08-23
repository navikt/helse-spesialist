package no.nav.helse.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MinimumSykdomsgradForDatabase(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdering: Boolean,
    val begrunnelse: String,
    val initierendeVedtaksperiodeId: UUID,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiverForDatabase>,
    val opprettet: LocalDateTime,
) {
    data class MinimumSykdomsgradArbeidsgiverForDatabase(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )
}
