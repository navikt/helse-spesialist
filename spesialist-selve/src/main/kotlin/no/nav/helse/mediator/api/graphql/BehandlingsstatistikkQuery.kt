package no.nav.helse.mediator.api.graphql

import graphql.execution.DataFetcherResult
import no.nav.helse.mediator.api.graphql.schema.Behandlingsstatistikk
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator

class BehandlingsstatistikkQuery(private val behandlingsstatistikkMediator: BehandlingsstatistikkMediator) {

    fun behandlingsstatistikk(): DataFetcherResult<Behandlingsstatistikk> {
        val statistikk = Behandlingsstatistikk(behandlingsstatistikkMediator.getBehandlingsstatistikk())
        return DataFetcherResult.newResult<Behandlingsstatistikk>()
            .data(statistikk).build()
    }

}