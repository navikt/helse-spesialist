package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TildelingMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : TildelingMutationSchema {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun opprettTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiTildeling?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(ContextValues.SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.håndter(
                TildelOppgave(oppgaveId = oppgaveId.toLong()),
                saksbehandlerFraApi = saksbehandler,
                tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
            )
            byggRespons(ApiTildeling(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid))
        } catch (e: OppgaveTildeltNoenAndre) {
            byggFeilrespons(alleredeTildeltError(e))
        } catch (e: RuntimeException) {
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
                saksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
            )
            byggRespons(true)
        } catch (e: OppgaveIkkeTildelt) {
            byggRespons(false)
        } catch (e: RuntimeException) {
            byggRespons(false)
        }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return graphqlErrorException(500, message)
    }

    private fun alleredeTildeltError(error: OppgaveTildeltNoenAndre) = graphqlErrorException(error.httpkode.value, "Oppgave allerede tildelt", "tildeling" to error.tildeling)
}
