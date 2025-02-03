package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.time.LocalDate
import java.util.UUID

@GraphQLName("MinimumSykdomsgrad")
data class ApiMinimumSykdomsgrad(
    val aktorId: String,
    val fodselsnummer: String,
    val perioderVurdertOk: List<ApiPeriode>,
    val perioderVurdertIkkeOk: List<ApiPeriode>,
    val begrunnelse: String,
    val arbeidsgivere: List<ApiArbeidsgiver>,
    val initierendeVedtaksperiodeId: UUID,
) : HandlingFraApi {
    @GraphQLName("Arbeidsgiver")
    data class ApiArbeidsgiver(
        val organisasjonsnummer: String,
        val berortVedtaksperiodeId: UUID,
    )

    @GraphQLName("Periode")
    data class ApiPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
