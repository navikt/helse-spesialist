package no.nav.helse.spesialist.api.feilhåndtering

import graphql.ExceptionWhileDataFetching
import graphql.GraphqlErrorException
import graphql.execution.SimpleDataFetcherExceptionHandler
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.loggWarnThrowable

class SpesialistDataFetcherExceptionHandler : SimpleDataFetcherExceptionHandler() {
    override fun logException(
        error: ExceptionWhileDataFetching,
        exception: Throwable,
    ) {
        val httpCode = (exception as? GraphqlErrorException)?.extensions?.get("code") as? Int
        if (httpCode != null && (httpCode in 400..499)) {
            loggWarnThrowable("Returnerer klientfeil (kode $httpCode) for GraphQL-kall til ${error.path}", exception)
        } else {
            loggThrowable("Uhåndtert feil ved GraphQL-kall til ${error.path}", exception)
        }
    }
}
