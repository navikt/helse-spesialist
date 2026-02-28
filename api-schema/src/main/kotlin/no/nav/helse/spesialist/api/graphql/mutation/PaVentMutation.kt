package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import java.time.LocalDate

interface PaVentMutationSchema : Mutation {
    fun leggPaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>? = emptyList(),
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?>

    fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?>

    fun endrePaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?>
}

class PaVentMutation(
    private val handler: PaVentMutationSchema,
) : PaVentMutationSchema by handler
