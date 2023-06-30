package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler

class OpptegnelseQuery(
    private val saksbehandlerMediator: SaksbehandlerMediator
): Query {

    @Suppress("unused")
    fun hentOpptegnelser(
        sekvensId: Int? = null,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<List<Opptegnelse>> {
        val saksbehandler = environment.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER.key)
        val opptegnelser =
            if (sekvensId != null) saksbehandlerMediator.hentAbonnerteOpptegnelser(saksbehandler, sekvensId)
            else saksbehandlerMediator.hentAbonnerteOpptegnelser(saksbehandler)

        return DataFetcherResult.newResult<List<Opptegnelse>>().data(opptegnelser).build()
    }
}