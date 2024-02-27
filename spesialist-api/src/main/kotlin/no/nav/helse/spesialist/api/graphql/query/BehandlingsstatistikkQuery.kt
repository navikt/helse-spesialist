package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import no.nav.helse.mediator.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.graphql.schema.Behandlingsstatistikk

class BehandlingsstatistikkQuery(private val behandlingsstatistikkMediator: BehandlingsstatistikkMediator) {

    fun behandlingsstatistikk(): DataFetcherResult<Behandlingsstatistikk> {
        val statistikk = Behandlingsstatistikk(behandlingsstatistikkMediator.getBehandlingsstatistikk())
        return DataFetcherResult.newResult<Behandlingsstatistikk>()
            .data(statistikk).build()
    }

}