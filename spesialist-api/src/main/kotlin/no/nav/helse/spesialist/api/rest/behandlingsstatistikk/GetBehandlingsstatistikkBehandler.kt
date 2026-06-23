package no.nav.helse.spesialist.api.rest.behandlingsstatistikk

import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.rest.ApiAntall
import no.nav.helse.spesialist.api.rest.ApiBehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.Behandlingsstatistikk
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags

class GetBehandlingsstatistikkBehandler(
    private val behandlingsstatistikkService: BehandlingsstatistikkService,
) : GetBehandler<Behandlingsstatistikk, ApiBehandlingsstatistikkResponse, ApiGetBehandlingsstatistikkErrorCode> {
    override val tag = Tags.BEHANDLINGSSTATISTIKK

    override fun behandle(
        resource: Behandlingsstatistikk,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiBehandlingsstatistikkResponse, ApiGetBehandlingsstatistikkErrorCode> =
        RestResponse.OK(
            behandlingsstatistikkService.getBehandlingsstatistikk().tilApiBehandlingsstatistikk(),
        )

    private fun no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse.tilApiBehandlingsstatistikk() =
        ApiBehandlingsstatistikkResponse(
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
}

enum class ApiGetBehandlingsstatistikkErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode
