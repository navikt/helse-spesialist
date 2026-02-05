package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.SendIReturResult
import no.nav.helse.spesialist.api.graphql.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.application.logg.kanskjeMedMdc
import no.nav.helse.spesialist.domain.Saksbehandler

class TotrinnsvurderingMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : TotrinnsvurderingMutationSchema {
    override fun sendTilGodkjenningV2(
        oppgavereferanse: String,
        vedtakBegrunnelse: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        kanskjeMedMdc(saksbehandlerMediator.finnMdcParametreForOppgaveId(oppgavereferanse)) {
            val behandlendeSaksbehandler: Saksbehandler = env.graphQlContext.get(SAKSBEHANDLER)

            when (
                val result =
                    saksbehandlerMediator.håndterTotrinnsvurdering(
                        oppgavereferanse = oppgavereferanse.toLong(),
                        saksbehandler = behandlendeSaksbehandler,
                        begrunnelse = vedtakBegrunnelse,
                    )
            ) {
                is SendTilGodkjenningResult.Feil.KunneIkkeFinnePerioderTilBehandling -> {
                    byggErrorRespons(
                        "Kunne ikke håndtere totrinnsvurdering, ukjent feil",
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

                SendTilGodkjenningResult.Ok -> byggRespons(true)
            }
        }

    override fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        kanskjeMedMdc(saksbehandlerMediator.finnMdcParametreForOppgaveId(oppgavereferanse)) {
            val besluttendeSaksbehandler: Saksbehandler = env.graphQlContext.get(SAKSBEHANDLER)

            when (
                val result =
                    saksbehandlerMediator.sendIRetur(
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

                SendIReturResult.Ok -> byggRespons(true)
            }
        }

    private fun byggErrorRespons(
        message: String,
        statusCode: HttpStatusCode,
    ): DataFetcherResult<Boolean?> = byggFeilrespons(graphqlErrorException(statusCode.value, message))
}
