package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate

@GraphQLName("DatoPeriode")
data class ApiDatoPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
