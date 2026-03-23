package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.time.LocalDate
import java.util.UUID

@GraphQLName("Skjonnsfastsettelse")
data class ApiSkjonnsfastsettelse(
    val aktorId: String,
    val fodselsnummer: String,
    val skjaringstidspunkt: LocalDate,
    val arbeidsgivere: List<ApiSkjonnsfastsettelseArbeidsgiver>,
    val vedtaksperiodeId: UUID,
) : HandlingFraApi {
    @GraphQLName("SkjonnsfastsettelseArbeidsgiver")
    data class ApiSkjonnsfastsettelseArbeidsgiver(
        val organisasjonsnummer: String,
        val arlig: Double,
        val fraArlig: Double,
        val arsak: String,
        val type: ApiSkjonnsfastsettelseType,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val lovhjemmel: ApiLovhjemmel?,
        val initierendeVedtaksperiodeId: String?,
    ) {
        @GraphQLName("SkjonnsfastsettelseType")
        enum class ApiSkjonnsfastsettelseType {
            OMREGNET_ARSINNTEKT,
            RAPPORTERT_ARSINNTEKT,
            ANNET,
        }
    }
}
