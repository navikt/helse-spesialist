package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Avslagstype")
enum class ApiAvslagstype {
    AVSLAG,
    DELVIS_AVSLAG,
}
