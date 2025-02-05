package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse

interface SkjonnsfastsettelseMutationSchema : Mutation {
    fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: ApiSkjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class SkjonnsfastsettelseMutation(private val handler: SkjonnsfastsettelseMutationSchema) :
    SkjonnsfastsettelseMutationSchema by handler
