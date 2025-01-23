package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.PaVentRequest
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.LoggerFactory

class TotrinnsvurderingMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
) : Mutation {
    companion object {
        private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun sendTilGodkjenningV2(
        oppgavereferanse: String,
        vedtakUtfall: VedtakUtfall,
        vedtakBegrunnelse: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val behandlendeSaksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            val result = saksbehandlerhåndterer.håndterTotrinnsvurdering(oppgavereferanse.toLong())

            return@withContext when (result) {
                is SendTilGodkjenningResult.Feil.KunneIkkeHåndtereTotrinnsvurdering ->
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
                SendTilGodkjenningResult.Ok -> {
                    try {
                        saksbehandlerhåndterer.håndterVedtakBegrunnelse(
                            oppgaveId = oppgavereferanse.toLong(),
                            saksbehandlerFraApi = behandlendeSaksbehandler,
                            utfall = vedtakUtfall,
                            begrunnelse = vedtakBegrunnelse,
                        )
                        oppgavehåndterer.sendTilBeslutter(oppgavereferanse.toLong(), behandlendeSaksbehandler)
                        saksbehandlerhåndterer.påVent(
                            PaVentRequest.FjernPaVentUtenHistorikkinnslag(oppgavereferanse.toLong()),
                            behandlendeSaksbehandler,
                        )
                    } catch (modellfeil: Modellfeil) {
                        return@withContext DataFetcherResult
                            .newResult<Boolean>()
                            .error(
                                GraphqlErrorException
                                    .newErrorException()
                                    .message("Feil ved sending til beslutter: ${modellfeil.message}")
                                    .extensions(mapOf("code" to modellfeil.httpkode))
                                    .build(),
                            ).data(false)
                            .build()
                    }

                    sikkerlogg.info(
                        "Oppgave med {} sendes til godkjenning av saksbehandler med {}",
                        StructuredArguments.kv("oppgaveId", oppgavereferanse),
                        StructuredArguments.kv("oid", behandlendeSaksbehandler.oid),
                    )

                    totrinnsvurderinghåndterer.avventerTotrinnsvurdering(oppgavereferanse.toLong(), behandlendeSaksbehandler)

                    log.info("OppgaveId $oppgavereferanse sendt til godkjenning")

                    DataFetcherResult.newResult<Boolean>().data(true).build()
                }
            }
        }

    @Suppress("unused")
    suspend fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val besluttendeSaksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)

            sikkerlogg.info(
                "Oppgave med {} sendes i retur av beslutter med {}",
                StructuredArguments.kv("oppgaveId", oppgavereferanse),
                StructuredArguments.kv("oid", besluttendeSaksbehandler.oid),
            )

            oppgavehåndterer.sendIRetur(oppgavereferanse.toLong(), besluttendeSaksbehandler)
            saksbehandlerhåndterer.påVent(
                PaVentRequest.FjernPaVentUtenHistorikkinnslag(oppgavereferanse.toLong()),
                besluttendeSaksbehandler,
            )

            totrinnsvurderinghåndterer.totrinnsvurderingRetur(
                oppgaveId = oppgavereferanse.toLong(),
                saksbehandlerFraApi = besluttendeSaksbehandler,
                notattekst = notatTekst,
            )

            log.info("OppgaveId $oppgavereferanse sendt i retur")

            DataFetcherResult.newResult<Boolean>().data(true).build()
        }
}
