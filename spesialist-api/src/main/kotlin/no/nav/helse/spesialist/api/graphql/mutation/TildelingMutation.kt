package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_OID
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TildelingMutation(
    private val tildelingService: TildelingService,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val notatMediator: NotatMediator,
) : Mutation {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun opprettTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Tildeling?> {
        val saksbehandler= env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(ContextValues.SAKSBEHANDLER.key).value
        return withContext(Dispatchers.IO) {
            try {
                saksbehandlerhåndterer.håndter(TildelOppgave(oppgaveId.toLong()), saksbehandler)
                newResult<Tildeling?>().data(
                    Tildeling(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid.toString(), true)
                ).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<Tildeling?>().error(alleredeTildeltError(e)).build()
            } catch (e: RuntimeException) {
                newResult<Tildeling?>().error(getUpdateError(oppgaveId)).build()
            }
        }
    }

    @Suppress("unused")
    suspend fun fjernTildeling(oppgaveId: String): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val result = tildelingService.fjernTildeling(oppgaveId.toLong())
        newResult<Boolean>().data(result).build()
    }

    @Suppress("unused")
    suspend fun leggPaaVent(
        oppgaveId: String,
        notatTekst: String,
        notatType: NotatType,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Tildeling?> {
        val saksbehandler= env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(ContextValues.SAKSBEHANDLER.key).value
        return withContext(Dispatchers.IO) {
            val saksbehandlerOid = UUID.fromString(env.graphQlContext.get(SAKSBEHANDLER_OID.key))

            try {
                notatMediator.lagreForOppgaveId(oppgaveId.toLong(), notatTekst, saksbehandlerOid, notatType)
                val tildeling = tildelingService.leggOppgavePåVent(oppgaveId = oppgaveId.toLong(), saksbehandler)
                newResult<Tildeling?>().data(
                    Tildeling(
                        navn = tildeling.navn,
                        oid = tildeling.oid.toString(),
                        epost = tildeling.epost,
                        paaVent = tildeling.påVent,
                    )
                ).build()
            } catch (e: OppgaveIkkeTildelt) {
                newResult<Tildeling?>().error(ikkeTildeltError(e)).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<Tildeling?>().error(tildeltNoenAndreError(e)).build()
            } catch (e: RuntimeException) {
                newResult<Tildeling?>().error(getUpdateError(oppgaveId)).build()
            }
        }
    }

    @Suppress("unused")
    suspend fun fjernPaaVent(oppgaveId: String, env: DataFetchingEnvironment): DataFetcherResult<Tildeling?> {
        val saksbehandler= env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(ContextValues.SAKSBEHANDLER.key).value
        return withContext(Dispatchers.IO) {
            try {
                val tildeling = tildelingService.fjernPåVent(oppgaveId.toLong(), saksbehandler)
                newResult<Tildeling?>().data(
                    Tildeling(
                        navn = tildeling.navn,
                        oid = tildeling.oid.toString(),
                        epost = tildeling.epost,
                        paaVent = tildeling.påVent,
                    )
                ).build()
            } catch (e: OppgaveIkkeTildelt) {
                newResult<Tildeling?>().error(ikkeTildeltError(e)).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<Tildeling?>().error(tildeltNoenAndreError(e)).build()
            }
        }
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
            .build()
    }

    private fun alleredeTildeltError(error: OppgaveTildeltNoenAndre): GraphQLError {
        return newErrorException()
            .message("Oppgave allerede tildelt")
            .extensions(mapOf("code" to error.httpkode, "tildeling" to error.tildeling))
            .build()
    }

    private fun tildeltNoenAndreError(error: OppgaveTildeltNoenAndre): GraphQLError {
        return newErrorException()
            .message("Oppgave tildelt noen andre")
            .extensions(mapOf("code" to error.httpkode, "tildeling" to error.tildeling))
            .build()
    }

    private fun ikkeTildeltError(error: OppgaveIkkeTildelt): GraphQLError {
        return newErrorException()
            .message("Oppgave ikke tildelt")
            .extensions(mapOf("code" to error.httpkode))
            .build()
    }
}
