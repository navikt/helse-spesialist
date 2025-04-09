package no.nav.helse.spesialist.api.feilhåndtering

import graphql.ExceptionWhileDataFetching
import graphql.execution.SimpleDataFetcherExceptionHandler
import no.nav.helse.spesialist.application.logg.sikkerlogg

class SpesialistDataFetcherExceptionHandler : SimpleDataFetcherExceptionHandler() {
    override fun logException(
        error: ExceptionWhileDataFetching,
        exception: Throwable,
    ) {
        sikkerlogg.error(error.message, exception)
    }
}
