package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler

class OpptegnelseMutation(
    private val saksbehandlerMediator: SaksbehandlerMediator
): Mutation {

    @Suppress("unused")
    fun abonner(
        personidentifikator: String,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = environment.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER.key)
        saksbehandlerMediator.opprettAbonnement(saksbehandler, personidentifikator)
        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}