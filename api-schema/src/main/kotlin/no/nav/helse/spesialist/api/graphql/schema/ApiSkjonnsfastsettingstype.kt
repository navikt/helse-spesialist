package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Skjonnsfastsettingstype")
enum class ApiSkjonnsfastsettingstype {
    OMREGNET_ARSINNTEKT,
    RAPPORTERT_ARSINNTEKT,
    ANNET,
}
