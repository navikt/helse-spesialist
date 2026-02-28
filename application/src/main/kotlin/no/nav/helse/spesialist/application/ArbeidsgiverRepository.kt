package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator

interface ArbeidsgiverRepository {
    fun lagre(arbeidsgiver: Arbeidsgiver)

    fun finn(id: ArbeidsgiverIdentifikator): Arbeidsgiver?

    fun finnAlle(ider: Set<ArbeidsgiverIdentifikator>): List<Arbeidsgiver>
}
