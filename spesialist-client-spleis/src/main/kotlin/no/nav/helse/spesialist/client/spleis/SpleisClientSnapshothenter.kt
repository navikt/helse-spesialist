package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson

class SpleisClientSnapshothenter(
    private val spleisClient: SpleisClient,
) : Snapshothenter {
    override fun hentPerson(fødselsnummer: String): SnapshotPerson? = spleisClient.hentPerson(fødselsnummer)?.tilSnapshotPerson()
}
