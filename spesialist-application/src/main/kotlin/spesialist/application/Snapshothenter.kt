package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.application.snapshot.SnapshotPerson

interface Snapshothenter {
    fun hentPerson(fødselsnummer: String): SnapshotPerson?
}
