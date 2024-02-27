package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import no.nav.helse.mediator.IBehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.graphql.schema.Behandlingsstatistikk

class BehandlingsstatistikkQuery(private val behandlingsstatistikkMediator: IBehandlingsstatistikkMediator) {

    fun behandlingsstatistikk(): DataFetcherResult<Behandlingsstatistikk> {
        val statistikk = Behandlingsstatistikk(behandlingsstatistikkMediator.getBehandlingsstatistikk())
        return DataFetcherResult.newResult<Behandlingsstatistikk>()
            .data(statistikk).build()
    }

}