package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator

class InMemoryArbeidsgiverRepository : ArbeidsgiverRepository {
    private val arbeidsgivere = mutableMapOf<ArbeidsgiverIdentifikator, Arbeidsgiver>()

    override fun lagre(arbeidsgiver: Arbeidsgiver) {
        arbeidsgivere[arbeidsgiver.id()] = arbeidsgiver
    }

    override fun finn(id: ArbeidsgiverIdentifikator) =
        arbeidsgivere[id]

    override fun finnAlle(ider: Set<ArbeidsgiverIdentifikator>) =
        ider.mapNotNull { arbeidsgivere[it] }

    fun alle() = arbeidsgivere.values.toList()
}
