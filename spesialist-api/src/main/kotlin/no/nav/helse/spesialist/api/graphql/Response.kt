package no.nav.helse.spesialist.api.graphql

import graphql.execution.DataFetcherResult.newResult

internal fun <T> byggRespons(data: T) = newResult<T>().data(data).build()
