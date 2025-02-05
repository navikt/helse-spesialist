package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO

interface VarselMutationSchema : Mutation {
    fun settVarselstatus(
        generasjonIdString: String,
        varselkode: String,
        ident: String,
        definisjonIdString: String? = null,
    ): DataFetcherResult<ApiVarselDTO?>
}

class VarselMutation(private val handler: VarselMutationSchema) : VarselMutationSchema by handler
