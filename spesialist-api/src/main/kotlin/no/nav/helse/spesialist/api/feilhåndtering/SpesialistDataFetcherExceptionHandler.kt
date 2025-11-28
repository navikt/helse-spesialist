package no.nav.helse.spesialist.api.feilhåndtering

import graphql.ExceptionWhileDataFetching
import graphql.execution.SimpleDataFetcherExceptionHandler
import no.nav.helse.spesialist.application.logg.loggThrowable

class SpesialistDataFetcherExceptionHandler : SimpleDataFetcherExceptionHandler() {
    override fun logException(
        error: ExceptionWhileDataFetching,
        exception: Throwable,
    ) {
        loggThrowable("Uhåndtert feil ved GraphQL-kall til ${error.path}", exception)
    }
}
