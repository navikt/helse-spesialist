package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.tildeling.TildelingService

class TildelingMutation(
    private val tildelingService: TildelingService,
) : Mutation {

    @Suppress("unused")
    suspend fun opprettTildeling(
        oppgaveId: String,
        saksbehandlerreferanse: String,
        epostadresse: String,
        navn: String,
        ident: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Tildeling> = withContext(Dispatchers.IO) {
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
        newResult<Tildeling>().data(
            Tildeling(
                navn = navn,
                oid = saksbehandlerreferanse,
                epost = epostadresse,
                reservert = tildeling.påVent
            )
        ).build()
    }

    @Suppress("unused")
    suspend fun fjernTildeling(oppgaveId: String): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val result = tildelingService.fjernTildeling(oppgaveId.toLong())
        newResult<Boolean>().data(result).build()
    }

}
