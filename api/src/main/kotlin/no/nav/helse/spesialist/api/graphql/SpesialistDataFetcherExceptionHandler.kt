package no.nav.helse.spesialist.api.graphql

import graphql.ExceptionWhileDataFetching
import graphql.GraphqlErrorException
import graphql.execution.SimpleDataFetcherExceptionHandler
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggWarn

class SpesialistDataFetcherExceptionHandler : SimpleDataFetcherExceptionHandler() {
    override fun logException(
        error: ExceptionWhileDataFetching,
        exception: Throwable,
    ) {
        val httpCode = (exception as? GraphqlErrorException)?.extensions?.get("code") as? Int
        if (httpCode != null && (httpCode in 400..499)) {
            loggWarn("Returnerer klientfeil (kode $httpCode) for GraphQL-kall til ${error.path}", exception)
        } else {
            loggError("Uhåndtert feil ved GraphQL-kall til ${error.path}", exception)
        }
    }
}
