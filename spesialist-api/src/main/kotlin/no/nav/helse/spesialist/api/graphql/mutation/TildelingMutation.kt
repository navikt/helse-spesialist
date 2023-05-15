package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.tildeling.TildelingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TildelingMutation(
    private val tildelingService: TildelingService,
) : Mutation {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private val logg: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Suppress("unused")
    suspend fun opprettTildeling(
        oppgaveId: String,
        saksbehandlerreferanse: String,
        epostadresse: String,
        navn: String,
        ident: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Tildeling?> = withContext(Dispatchers.IO) {
        logg.debug("Hallo fra opprettTildeling mutation")
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>("tilganger")
        val saksbehandlerOid = UUID.fromString(saksbehandlerreferanse)
        val tildeling = try { tildelingService.tildelOppgaveTilSaksbehandler(
                oppgaveId = oppgaveId.toLong(),
                saksbehandlerreferanse = saksbehandlerOid,
                epostadresse = epostadresse,
                navn = navn,
                ident = ident,
                saksbehandlerTilganger = tilganger
            )
        } catch (e: RuntimeException) {
            logg.error("Kunne ikke tildele: {}", kv("exception", e))
            null
        } ?: return@withContext newResult<Tildeling?>().error(getUpdateError(oppgaveId)).build()

        sikkerlogg.info("tildeling fra tildelingservice: {}", kv("tildeling", tildeling))
        logg.info("tildeling fra tildelingservice: {}", kv("tildeling", tildeling))
        val returnTildeling = Tildeling(
            navn = tildeling.navn,
            oid = tildeling.oid.toString(),
            epost = tildeling.epost,
            reservert = tildeling.påVent
        )
        sikkerlogg.info("returtildeling ting: {}", kv("returnTildeling", returnTildeling))
        newResult<Tildeling?>().data(returnTildeling).build()
    }

    @Suppress("unused")
    suspend fun fjernTildeling(oppgaveId: String): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val result = tildelingService.fjernTildeling(oppgaveId.toLong())
        newResult<Boolean>().data(result).build()
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to 500))
            .build()
    }

}
