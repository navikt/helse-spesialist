package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
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
