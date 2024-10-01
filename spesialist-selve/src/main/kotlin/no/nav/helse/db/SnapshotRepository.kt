package no.nav.helse.db

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson

interface SnapshotRepository {
    fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    )
}
