package no.nav.helse.spesialist.api.graphql

import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> byggRespons(data: T): DataFetcherResult<T?> = newResult<T>().data(data).build() as DataFetcherResult<T?>
