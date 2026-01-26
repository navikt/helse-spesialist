package no.nav.helse.spesialist.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import no.nav.helse.spesialist.application.logg.teamLogs

internal fun forbiddenError(fødselsnummer: String): GraphqlErrorException =
    graphqlErrorException(
        403,
        "Har ikke tilgang til person med fødselsnummer $fødselsnummer",
        "field" to "person",
    )

internal fun notFoundError(identifikator: String? = null): GraphqlErrorException =
    graphqlErrorException(
        404,
        "Finner ikke data for person med identifikator $identifikator",
        "field" to "person",
    )

internal fun personNotReadyError(
    fødselsnummer: String,
    aktørId: String,
): GraphqlErrorException =
    graphqlErrorException(
        409,
        "Person med fødselsnummer $fødselsnummer er ikke klar for visning ennå",
        "field" to "person",
        "persondata_hentes_for" to aktørId,
    )

internal fun <T> notFound(message: String) = dataFetcherError<T>(404, message)

internal fun <T> conflict(message: String) = dataFetcherError<T>(409, message)

internal fun <T> internalServerError(message: String) = dataFetcherError<T>(500, message)

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
