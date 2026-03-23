package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@GraphQLName("Arbeidsgiverrefusjon")
data class ApiArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<ApiRefusjonselement>,
)

@GraphQLName("Refusjonselement")
data class ApiRefusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID,
)

@GraphQLName("ArbeidsgiverrefusjonV2")
data class ApiArbeidsgiverrefusjonV2(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<ApiRefusjonselementV2>,
)

@GraphQLName("RefusjonselementV2")
data class ApiRefusjonselementV2(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: BigDecimal,
    val meldingsreferanseId: UUID,
)
