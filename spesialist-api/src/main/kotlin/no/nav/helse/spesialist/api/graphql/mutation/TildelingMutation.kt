package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
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

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val logg: Logger = LoggerFactory.getLogger(this.javaClass.simpleName)

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
        val tildeling = tildelingService.tildelOppgaveTilSaksbehandler(
            oppgaveId = oppgaveId.toLong(),
            saksbehandlerreferanse = saksbehandlerOid,
            epostadresse = epostadresse,
            navn = navn,
            ident = ident,
            saksbehandlerTilganger = tilganger
        )
        sikkerLogg.info("tildeling fra tildelingservice: {}", kv("tildeling", tildeling))
        logg.info("tildeling fra tildelingservice: {}", kv("tildeling", tildeling))
        val returnTildeling = Tildeling(
            navn = tildeling.navn,
            oid = tildeling.oid.toString(),
            epost = tildeling.epost,
            reservert = tildeling.påVent
        )
        sikkerLogg.info("returtildeling ting: {}", kv("returnTildeling", returnTildeling))
        newResult<Tildeling?>().data(returnTildeling).build()
    }

    @Suppress("unused")
    suspend fun fjernTildeling(oppgaveId: String): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val result = tildelingService.fjernTildeling(oppgaveId.toLong())
        newResult<Boolean>().data(result).build()
    }

}
