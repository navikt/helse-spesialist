package no.nav.helse.spesialist.api.snapshot

import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.schema.Personinfo

class SnapshotMediator(private val snapshotDao: SnapshotApiDao, private val snapshotClient: SnapshotClient) {

    private fun oppdaterSnapshot(fødselsnummer: String) {
        if (snapshotDao.utdatert(fødselsnummer)) {
            snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
        }
    }

    fun hentSnapshot(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? {
        oppdaterSnapshot(fødselsnummer)
        return snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
    }

}