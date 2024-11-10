package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.graphql.schema.Behandlingsstatistikk

class BehandlingsstatistikkQuery(private val behandlingsstatistikkMediator: IBehandlingsstatistikkService) : Query {
    @Suppress("unused")
    fun behandlingsstatistikk(): DataFetcherResult<Behandlingsstatistikk> {
        val statistikk = Behandlingsstatistikk(behandlingsstatistikkMediator.getBehandlingsstatistikk())
        return DataFetcherResult.newResult<Behandlingsstatistikk>()
            .data(statistikk).build()
    }
}
