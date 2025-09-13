package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.domain.Saksbehandler

class OpptegnelseMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OpptegnelseMutationSchema {
    override fun opprettAbonnement(
        personidentifikator: String,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = environment.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
        saksbehandlerMediator.opprettAbonnement(saksbehandler, personidentifikator)
        return byggRespons(true)
    }
}
