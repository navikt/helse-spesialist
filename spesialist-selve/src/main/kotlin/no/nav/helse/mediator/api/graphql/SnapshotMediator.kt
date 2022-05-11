package no.nav.helse.mediator.api.graphql

import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.SnapshotDao

class SnapshotMediator(private val snapshotDao: SnapshotDao, private val snapshotClient: SnapshotClient) {

    private fun oppdaterSnapshot(fødselsnummer: String) {
        if (snapshotDao.utdatert(fødselsnummer)) {
            snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
        }
    }

    internal fun hentSnapshot(fødselsnummer: String): Pair<PersoninfoDto, GraphQLPerson>? {
        oppdaterSnapshot(fødselsnummer)
        return snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
    }

}