package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandlingsstatistikk

interface BehandlingsstatistikkQuerySchema : Query {
    fun behandlingsstatistikk(): DataFetcherResult<ApiBehandlingsstatistikk>
}

class BehandlingsstatistikkQuery(private val handler: BehandlingsstatistikkQuerySchema) : BehandlingsstatistikkQuerySchema by handler
