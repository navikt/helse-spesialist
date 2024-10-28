package no.nav.helse.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MinimumSykdomsgradForDatabase(
    val id: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdering: Boolean,
    val begrunnelse: String,
    val initierendeVedtaksperiodeId: UUID,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiverForDatabase>,
    override val opprettet: LocalDateTime,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
    override val vedtaksperiodeId: UUID = initierendeVedtaksperiodeId

    data class MinimumSykdomsgradArbeidsgiverForDatabase(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )
}
