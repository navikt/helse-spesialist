package no.nav.helse.db.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MinimumSykdomsgradForDatabase(
    override val eksternHendelseId: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val perioderVurdertOk: List<MinimumSykdomsgradPeriodeForDatabase>,
    val perioderVurdertIkkeOk: List<MinimumSykdomsgradPeriodeForDatabase>,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiverForDatabase>,
    override val opprettet: LocalDateTime,
    override val saksbehandlerOid: UUID,
    override val ferdigstilt: Boolean,
) : OverstyringForDatabase {
    data class MinimumSykdomsgradArbeidsgiverForDatabase(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )

    data class MinimumSykdomsgradPeriodeForDatabase(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
