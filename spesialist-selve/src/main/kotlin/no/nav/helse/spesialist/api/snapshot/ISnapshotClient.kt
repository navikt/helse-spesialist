package no.nav.helse.spesialist.api.snapshot

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson

interface ISnapshotClient {
    fun hentSnapshot(fnr: String): GraphQLPerson?
}
