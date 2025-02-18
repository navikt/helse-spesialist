package no.nav.helse.spesialist.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import no.nav.helse.spesialist.application.logg.sikkerlogg

internal fun forbiddenError(fødselsnummer: String): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Har ikke tilgang til person med fødselsnummer $fødselsnummer")
        .extensions(mapOf("code" to 403, "field" to "person"))
        .build()

internal fun notFoundError(identifikator: String? = null): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Finner ikke data for person med identifikator $identifikator")
        .extensions(mapOf("code" to 404, "field" to "person"))
        .build()

internal fun personNotReadyError(
    fødselsnummer: String,
    aktørId: String,
): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Person med fødselsnummer $fødselsnummer er ikke klar for visning ennå")
        .extensions(mapOf("code" to 409, "field" to "person", "persondata_hentes_for" to aktørId))
        .build()

internal fun <T> notFound(message: String) = dataFetcherError<T>(404, message)

internal fun <T> conflict(message: String) = dataFetcherError<T>(409, message)

internal fun <T> internalServerError(message: String) = dataFetcherError<T>(500, message)

private fun <T> dataFetcherError(
    httpCode: Int,
    message: String,
): DataFetcherResult<T> =
    newResult<T>()
        .error(
            GraphqlErrorException.newErrorException()
                .message(message)
                .extensions(mapOf("code" to httpCode))
                .build(),
        ).build()
        .also {
            sikkerlogg.error("Returnerer $httpCode-feil for GraphQL-operasjon: $message")
        }
