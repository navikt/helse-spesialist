package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Saksbehandler

class TildelingMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : TildelingMutationSchema {
    override fun opprettTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiTildeling?> {
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.håndter(
                TildelOppgave(oppgaveId = oppgaveId.toLong()),
                saksbehandler = saksbehandler,
                brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
            )
            byggRespons(ApiTildeling(saksbehandler.navn, saksbehandler.epost, saksbehandler.id.value))
        } catch (e: OppgaveTildeltNoenAndre) {
            byggFeilrespons(alleredeTildeltError(e))
        } catch (_: RuntimeException) {
            byggFeilrespons(getUpdateError(oppgaveId))
        }
    }

    override fun fjernTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        try {
            saksbehandlerMediator.håndter(
                handlingFraApi = AvmeldOppgave(oppgaveId = oppgaveId.toLong()),
                saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
            )
            byggRespons(true)
        } catch (_: OppgaveIkkeTildelt) {
            byggRespons(false)
        } catch (_: RuntimeException) {
            byggRespons(false)
        }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        teamLogs.error(message)
        return graphqlErrorException(500, message)
    }

    private fun alleredeTildeltError(error: OppgaveTildeltNoenAndre) = graphqlErrorException(error.httpkode.value, "Oppgave allerede tildelt", "tildeling" to error.tildeling)
}
