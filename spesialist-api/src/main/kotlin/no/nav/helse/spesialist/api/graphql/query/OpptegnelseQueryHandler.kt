package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

class OpptegnelseQueryHandler(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) : OpptegnelseQuerySchema {
    override fun opptegnelser(
        sekvensId: Int?,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiOpptegnelse>> {
        val saksbehandler = environment.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val opptegnelser =
            if (sekvensId != null) {
                saksbehandlerhåndterer.hentAbonnerteOpptegnelser(saksbehandler, sekvensId)
            } else {
                saksbehandlerhåndterer.hentAbonnerteOpptegnelser(saksbehandler)
            }

        return DataFetcherResult.newResult<List<ApiOpptegnelse>>().data(opptegnelser).build()
    }
}
