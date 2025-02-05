package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
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
            is SendTilGodkjenningResult.Feil.KunneIkkeFinnePerioderTilBehandling ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Kunne ikke håndtere totrinnsvurdering, ukjennt feil")
                            .extensions(mapOf("code" to 500))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message(result.modellfeil.message)
                            .extensions(mapOf("code" to result.modellfeil.httpkode))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Feil ved håndtering av begrunnelse: ${result.e.message}")
                            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Feil ved sending til beslutter: ${result.modellfeil.message}")
                            .extensions(mapOf("code" to result.modellfeil.httpkode))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Kunne ikke fjerne fra på vent: ${result.modellfeil.message}")
                            .extensions(mapOf("code" to result.modellfeil.httpkode))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Feil ved fjerning av på vent: ${result.e.message}")
                            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Feil ved sending til beslutter: ${result.e.message}")
                            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
                            .build(),
                    ).data(false)
                    .build()

            is SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk ->
                DataFetcherResult
                    .newResult<Boolean>()
                    .error(
                        GraphqlErrorException
                            .newErrorException()
                            .message("Feil ved oppretting av periodehistorikk: ${result.e.message}")
                            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
                            .build(),
                    ).data(false)
                    .build()

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

        sikkerlogg.info(
            "Oppgave med {} sendes i retur av beslutter med {}",
            StructuredArguments.kv("oppgaveId", oppgavereferanse),
            StructuredArguments.kv("oid", besluttendeSaksbehandler.oid),
        )

        apiOppgaveService.sendIRetur(oppgavereferanse.toLong(), besluttendeSaksbehandler)
        saksbehandlerhåndterer.påVent(
            ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag(oppgavereferanse.toLong()),
            besluttendeSaksbehandler,
        )

        totrinnsvurderinghåndterer.totrinnsvurderingRetur(
            oppgaveId = oppgavereferanse.toLong(),
            saksbehandlerFraApi = besluttendeSaksbehandler,
            notattekst = notatTekst,
        )

        log.info("OppgaveId $oppgavereferanse sendt i retur")

        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}
