package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandlingsstatistikk

class BehandlingsstatistikkQuery(private val behandlingsstatistikkMediator: IBehandlingsstatistikkService) : Query {
    @Suppress("unused")
    fun behandlingsstatistikk(): DataFetcherResult<ApiBehandlingsstatistikk> {
        val statistikk = ApiBehandlingsstatistikk(behandlingsstatistikkMediator.getBehandlingsstatistikk())
        return DataFetcherResult.newResult<ApiBehandlingsstatistikk>()
            .data(statistikk).build()
    }
}
