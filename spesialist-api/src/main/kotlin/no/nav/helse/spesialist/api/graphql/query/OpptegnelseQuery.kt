package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

class OpptegnelseQuery(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer
): Query {

    @Suppress("unused")
    fun hentOpptegnelser(sekvensId: Int? = null, environment: DataFetchingEnvironment): DataFetcherResult<List<Opptegnelse>> {
        val saksbehandler = environment.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(SAKSBEHANDLER.key).value
        val opptegnelser =
            if (sekvensId != null) saksbehandlerhåndterer.hentAbonnerteOpptegnelser(saksbehandler, sekvensId)
            else saksbehandlerhåndterer.hentAbonnerteOpptegnelser(saksbehandler)

        return DataFetcherResult.newResult<List<Opptegnelse>>().data(opptegnelser).build()
    }
}