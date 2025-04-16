package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.measureTimedValue

class OppgaverQueryHandler(private val apiOppgaveService: ApiOppgaveService) : OppgaverQuerySchema {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override suspend fun behandledeOppgaverFeed(
        offset: Int,
        limit: Int,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val behandledeOppgaver =
            apiOppgaveService.behandledeOppgaver(
                saksbehandlerFraApi = saksbehandler,
                offset = offset,
                limit = limit,
            )

        return byggRespons(behandledeOppgaver)
    }

    override suspend fun behandledeOppgaverFeedV2(
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val behandledeOppgaver =
            apiOppgaveService.behandledeOppgaver(
                saksbehandlerFraApi = saksbehandler,
                offset = offset,
                limit = limit,
                fom = fom,
                tom = tom,
            )

        return byggRespons(behandledeOppgaver)
    }

    override suspend fun oppgaveFeed(
        offset: Int,
        limit: Int,
        sortering: List<ApiOppgavesortering>,
        filtrering: ApiFiltrering,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.debug("Henter OppgaverTilBehandling for ${saksbehandler.navn}")
        val (oppgaver, tid) =
            measureTimedValue {
                apiOppgaveService.oppgaver(
                    saksbehandlerFraApi = saksbehandler,
                    offset = offset,
                    limit = limit,
                    sortering = sortering,
                    filtrering = filtrering,
                )
            }
        sikkerLogg.debug("Query OppgaverTilBehandling er ferdig etter ${tid.inWholeMilliseconds} ms")
        val grense = 5000
        if (tid.inWholeMilliseconds > grense) {
            sikkerLogg.info("Det tok over $grense ms å hente oppgaver med disse filtrene: $filtrering")
        }

        return byggRespons(oppgaver)
    }

    override suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<ApiAntallOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.info("Henter AntallOppgaver for ${saksbehandler.navn}")
        val (antallOppgaver, tid) =
            measureTimedValue {
                apiOppgaveService.antallOppgaver(
                    saksbehandlerFraApi = saksbehandler,
                )
            }
        sikkerLogg.debug("Query antallOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")

        return byggRespons(antallOppgaver)
    }
}
