package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("SelvstendigNaering")
data class ApiSelvstendigNaering(
    val behandlinger: List<ApiBehandling>,
    val overstyringer: List<ApiOverstyring>,
)
