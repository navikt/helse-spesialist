package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson

internal class SpleisRestClientSnapshothenter(
    private val spleisRestClient: SpleisRestClient,
) : Snapshothenter {
    override fun hentPerson(fødselsnummer: String): SnapshotPerson? = spleisRestClient.hentPerson(fødselsnummer)?.tilSnapshotPerson()
}
