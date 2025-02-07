package no.nav.helse.db.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MinimumSykdomsgradForDatabase(
    override val id: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val perioderVurdertOk: List<MinimumSykdomsgradPeriodeForDatabase>,
    val perioderVurdertIkkeOk: List<MinimumSykdomsgradPeriodeForDatabase>,
    val begrunnelse: String,
    val initierendeVedtaksperiodeId: UUID,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiverForDatabase>,
    override val opprettet: LocalDateTime,
    override val saksbehandlerOid: UUID,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
    override val vedtaksperiodeId: UUID = initierendeVedtaksperiodeId

    data class MinimumSykdomsgradArbeidsgiverForDatabase(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )

    data class MinimumSykdomsgradPeriodeForDatabase(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
