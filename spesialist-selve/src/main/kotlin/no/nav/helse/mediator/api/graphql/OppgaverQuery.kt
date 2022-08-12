package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDateTime
import no.nav.helse.mediator.api.graphql.schema.Oppgaver
import no.nav.helse.mediator.api.graphql.schema.Pagination
import no.nav.helse.mediator.api.graphql.schema.tilOppgaver
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.oppgave.OppgaveMediator

class OppgaverQuery(private val oppgaveMediator: OppgaveMediator) : Query {

    fun oppgaver(first: Int, after: String, env: DataFetchingEnvironment): DataFetcherResult<Oppgaver> {
        val cursor = try {
            LocalDateTime.parse(after)
        } catch (exception: Exception) {
            return DataFetcherResult.newResult<Oppgaver>().error(getParseCursorError()).build()
        }

        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>("tilganger")
        val oppgaver = Oppgaver(
            oppgaver = oppgaveMediator.hentOppgaver(tilganger, cursor, first).tilOppgaver(),
            pagination = Pagination(
                cursor = after,
                currentPage = 1,
                totalPages = 1
            )
        )

        return DataFetcherResult.newResult<Oppgaver>().data(oppgaver).build()
    }

    private fun getParseCursorError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Mottok cursor med uventet format")
        .extensions(mapOf("code" to 400))
        .build()

}