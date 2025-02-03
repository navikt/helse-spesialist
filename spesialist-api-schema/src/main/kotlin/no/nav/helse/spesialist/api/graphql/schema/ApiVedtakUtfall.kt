package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("VedtakUtfall")
enum class ApiVedtakUtfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
}
