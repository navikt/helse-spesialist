package no.nav.helse.db.api

import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson

interface SnapshotApiDao {
    fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>?

    fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    ): Int
}
