package no.nav.helse.spesialist.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import no.nav.helse.spesialist.application.logg.teamLogs

internal fun <T> conflict(message: String) = dataFetcherError<T>(409, message)

private fun <T> dataFetcherError(
    httpCode: Int,
    message: String,
    vararg extensions: Pair<String, Any>,
): DataFetcherResult<T> {
    teamLogs.error("Returnerer $httpCode-feil for GraphQL-operasjon: $message")
    return byggFeilrespons(graphqlErrorException(httpCode, message, *extensions))
}

internal fun graphqlErrorException(
    httpCode: Int,
    message: String,
    vararg extensions: Pair<String, Any>,
): GraphqlErrorException = newErrorException().message(message).extensions(mapOf("code" to httpCode, *extensions)).build()

internal fun <T> byggFeilrespons(error: GraphQLError) = newResult<T>().error(error).build()
