package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.LoggerFactory

class TotrinnsvurderingMutationHandler(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val apiOppgaveService: ApiOppgaveService,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
) : TotrinnsvurderingMutationSchema {
    companion object {
        private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun sendTilGodkjenningV2(
        oppgavereferanse: String,
        vedtakUtfall: ApiVedtakUtfall,
        vedtakBegrunnelse: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val behandlendeSaksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)

        return when (
            val result =
                saksbehandlerhåndterer.håndterTotrinnsvurdering(
                    oppgavereferanse = oppgavereferanse.toLong(),
                    saksbehandlerFraApi = behandlendeSaksbehandler,
                    utfall = vedtakUtfall,
                    begrunnelse = vedtakBegrunnelse,
                )
        ) {
            is SendTilGodkjenningResult.Feil.KunneIkkeFinnePerioderTilBehandling -> {
                byggErrorRespons(
                    "Kunne ikke håndtere totrinnsvurdering, ukjennt feil",
                    HttpStatusCode.InternalServerError,
                )
            }

            is SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler -> {
                byggErrorRespons(
                    result.modellfeil.message,
                    result.modellfeil.httpkode,
                )
            }

            is SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse ->
                byggErrorRespons(
                    "Feil ved håndtering av begrunnelse: ${result.e.message}",
                    HttpStatusCode.InternalServerError,
                )

            is SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter ->
                byggErrorRespons(
                    "Feil ved sending til beslutter: ${result.modellfeil.message}",
                    result.modellfeil.httpkode,
                )

            is SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent ->
                byggErrorRespons(
                    "Kunne ikke fjerne fra på vent: ${result.modellfeil.message}",
                    result.modellfeil.httpkode,
                )

            is SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent ->
                byggErrorRespons(
                    "Feil ved fjerning av på vent: ${result.e.message}",
                    HttpStatusCode.InternalServerError,
                )

            is SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter ->
                byggErrorRespons(
                    "Feil ved sending til beslutter: ${result.e.message}",
                    HttpStatusCode.InternalServerError,
                )

            is SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk ->
                byggErrorRespons(
                    "Feil ved oppretting av periodehistorikk: ${result.e.message}",
                    HttpStatusCode.InternalServerError,
                )

            SendTilGodkjenningResult.Ok ->
                DataFetcherResult.newResult<Boolean>().data(true).build()
        }
    }

    override fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val besluttendeSaksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)

        return when (
            val result =
                saksbehandlerhåndterer.sendIRetur(
                    oppgavereferanse = oppgavereferanse.toLong(),
                    besluttendeSaksbehandler = besluttendeSaksbehandler,
                    notatTekst = notatTekst,
                )
        ) {
            is SendIReturResult.Feil.KunneIkkeLeggePåVent ->
                byggErrorRespons(
                    "Feil ved legg på vent: ${result.modellfeil.message}",
                    result.modellfeil.httpkode,
                )

            is SendIReturResult.Feil.KunneIkkeOppretteHistorikkinnslag ->
                byggErrorRespons(
                    "Feil ved oppretting av periodehistorikk: ${result.exception.message}",
                    HttpStatusCode.InternalServerError,
                )

            is SendIReturResult.Feil.KunneIkkeSendeIRetur ->
                byggErrorRespons(
                    "Feil ved oppretting av periodehistorikk: ${result.modellfeil.message}",
                    result.modellfeil.httpkode,
                )

            SendIReturResult.Ok -> DataFetcherResult.newResult<Boolean>().data(true).build()
        }
    }

    private fun byggErrorRespons(
        message: String,
        statusCode: HttpStatusCode,
    ): DataFetcherResult<Boolean> =
        DataFetcherResult
            .newResult<Boolean>()
            .error(
                GraphqlErrorException
                    .newErrorException()
                    .message(message)
                    .extensions(mapOf("code" to statusCode.value))
                    .build(),
            ).data(false)
            .build()
}
