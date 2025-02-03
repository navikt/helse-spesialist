package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.graphql.schema.ApiAntall
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandlingsstatistikk

class BehandlingsstatistikkQueryHandler(private val behandlingsstatistikkMediator: IBehandlingsstatistikkService) :
    BehandlingsstatistikkQuerySchema {
    @Suppress("unused")
    override fun behandlingsstatistikk(): DataFetcherResult<ApiBehandlingsstatistikk> =
        behandlingsstatistikkMediator.getBehandlingsstatistikk()
            .tilApiBehandlingsstatistikk()
            .tilDataFetcherResult()

    private fun BehandlingsstatistikkResponse.tilApiBehandlingsstatistikk() =
        ApiBehandlingsstatistikk(
            enArbeidsgiver = enArbeidsgiver.tilApiAntall(),
            flereArbeidsgivere = flereArbeidsgivere.tilApiAntall(),
            forstegangsbehandling = forstegangsbehandling.tilApiAntall(),
            forlengelser = forlengelser.tilApiAntall(),
            forlengelseIt = forlengelseIt.tilApiAntall(),
            utbetalingTilArbeidsgiver = utbetalingTilArbeidsgiver.tilApiAntall(),
            utbetalingTilSykmeldt = utbetalingTilSykmeldt.tilApiAntall(),
            faresignaler = faresignaler.tilApiAntall(),
            fortroligAdresse = fortroligAdresse.tilApiAntall(),
            stikkprover = stikkprover.tilApiAntall(),
            revurdering = revurdering.tilApiAntall(),
            delvisRefusjon = delvisRefusjon.tilApiAntall(),
            beslutter = beslutter.tilApiAntall(),
            egenAnsatt = egenAnsatt.tilApiAntall(),
            antallAnnulleringer = antallAnnulleringer,
            antallAvvisninger = antallAvvisninger,
        )

    private fun Statistikk.tilApiAntall() =
        ApiAntall(
            automatisk = automatisk,
            manuelt = manuelt,
            tilgjengelig = tilgjengelig,
        )

    private inline fun <reified T> T.tilDataFetcherResult(): DataFetcherResult<T> = DataFetcherResult.newResult<T>().data(this).build()
}
