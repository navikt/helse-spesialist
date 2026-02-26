package no.nav.helse.spleis.graphql

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson

data class HentSnapshotResult(
    val person: GraphQLPerson? = null,
)
