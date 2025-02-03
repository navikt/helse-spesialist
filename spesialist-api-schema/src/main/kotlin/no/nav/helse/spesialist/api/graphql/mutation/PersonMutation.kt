package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult

interface PersonMutationSchema : Mutation {
    suspend fun oppdaterPerson(fodselsnummer: String): DataFetcherResult<Boolean>
}

class PersonMutation(private val handler: PersonMutationSchema) : PersonMutationSchema by handler
