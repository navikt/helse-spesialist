package no.nav.helse.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SkjønnsfastsattSykepengegrunnlagForDatabase(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverForDatabase>,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
)

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
