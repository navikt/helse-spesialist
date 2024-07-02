package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

class OpptegnelseMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) : Mutation {
    @Suppress("unused")
    fun opprettAbonnement(
        personidentifikator: String,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = environment.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER.key)
        saksbehandlerhåndterer.opprettAbonnement(saksbehandler, personidentifikator)
        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}
