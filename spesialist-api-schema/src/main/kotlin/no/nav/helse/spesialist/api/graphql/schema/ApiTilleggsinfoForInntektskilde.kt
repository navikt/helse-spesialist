package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("TilleggsinfoForInntektskilde")
data class ApiTilleggsinfoForInntektskilde(
    val orgnummer: String,
    val navn: String,
)
