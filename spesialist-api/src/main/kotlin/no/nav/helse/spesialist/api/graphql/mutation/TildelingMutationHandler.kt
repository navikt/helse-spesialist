package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
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
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.håndter(TildelOppgave(oppgaveId.toLong()), saksbehandler)
            newResult<ApiTildeling?>().data(
                ApiTildeling(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid),
            ).build()
        } catch (e: OppgaveTildeltNoenAndre) {
            newResult<ApiTildeling?>().error(alleredeTildeltError(e)).build()
        } catch (e: RuntimeException) {
            newResult<ApiTildeling?>().error(getUpdateError(oppgaveId)).build()
        }
    }

    override fun fjernTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.håndter(AvmeldOppgave(oppgaveId.toLong()), saksbehandler)
            newResult<Boolean>().data(true).build()
        } catch (e: OppgaveIkkeTildelt) {
            newResult<Boolean>().data(false).build()
        } catch (e: RuntimeException) {
            newResult<Boolean>().data(false).build()
        }
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return graphqlErrorException(500, message)
    }

    private fun alleredeTildeltError(error: OppgaveTildeltNoenAndre) =
        graphqlErrorException(error.httpkode.value, "Oppgave allerede tildelt", "tildeling" to error.tildeling)
}
