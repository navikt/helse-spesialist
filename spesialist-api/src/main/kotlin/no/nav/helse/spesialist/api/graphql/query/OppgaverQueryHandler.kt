package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import java.time.LocalDate
import kotlin.time.measureTimedValue

class OppgaverQueryHandler(
    private val apiOppgaveService: ApiOppgaveService,
) : OppgaverQuerySchema {
    override suspend fun behandledeOppgaverFeed(
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver> {
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
        val behandledeOppgaver =
            apiOppgaveService.behandledeOppgaver(
                saksbehandler = saksbehandler,
                offset = offset,
                limit = limit,
                fom = fom,
                tom = tom,
            )

        return byggRespons(behandledeOppgaver)
    }

    override suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<ApiAntallOppgaver> {
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
        sikkerlogg.info("Henter AntallOppgaver for ${saksbehandler.navn}")
        val (antallOppgaver, tid) =
            measureTimedValue {
                apiOppgaveService.antallOppgaver(
                    saksbehandler = saksbehandler,
                )
            }
        sikkerlogg.debug("Query antallOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")

        return byggRespons(antallOppgaver)
    }
}
