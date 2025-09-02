package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLIgnore
interface SelvstendigNaeringSchema {
    fun generasjoner(): List<ApiGenerasjon>

    fun overstyringer(): List<ApiOverstyring>
}

@GraphQLName("SelvstendigNaering")
class ApiSelvstendigNaering(
    private val resolver: SelvstendigNaeringSchema,
) : SelvstendigNaeringSchema by resolver
