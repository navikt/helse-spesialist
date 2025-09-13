package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelse
import no.nav.helse.spesialist.domain.Saksbehandler

class OpptegnelseQueryHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OpptegnelseQuerySchema {
    override fun opptegnelser(
        sekvensId: Int?,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiOpptegnelse>> {
        val saksbehandler = environment.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
        val opptegnelser =
            if (sekvensId != null) {
                saksbehandlerMediator.hentAbonnerteOpptegnelser(saksbehandler, sekvensId)
            } else {
                saksbehandlerMediator.hentAbonnerteOpptegnelser(saksbehandler)
            }

        return byggRespons(opptegnelser)
    }
}
