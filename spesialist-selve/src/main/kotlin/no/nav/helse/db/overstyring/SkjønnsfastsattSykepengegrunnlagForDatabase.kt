package no.nav.helse.db.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SkjønnsfastsattSykepengegrunnlagForDatabase(
    override val id: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverForDatabase>,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val saksbehandlerOid: UUID,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
}

data class SkjønnsfastsattArbeidsgiverForDatabase(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double,
    val årsak: String,
    val type: SkjønnsfastsettingstypeForDatabase,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val lovhjemmel: LovhjemmelForDatabase?,
    val initierendeVedtaksperiodeId: String?,
)

enum class SkjønnsfastsettingstypeForDatabase {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}
