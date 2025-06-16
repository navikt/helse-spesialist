package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverId
import kotlin.random.Random

class InMemoryArbeidsgiverRepository : ArbeidsgiverRepository {
    private val arbeidsgivere = mutableMapOf<ArbeidsgiverId, Arbeidsgiver>()

    override fun lagre(arbeidsgiver: Arbeidsgiver) {
        val id = if (arbeidsgiver.harFåttTildeltId()) {
            arbeidsgiver.id()
        } else {
            ArbeidsgiverId(Random.nextInt()).also(arbeidsgiver::tildelId)
        }
        arbeidsgivere[id] = arbeidsgiver
    }

    override fun finn(id: ArbeidsgiverId) = arbeidsgivere[id]

    override fun finnForIdentifikator(identifikator: Arbeidsgiver.Identifikator) =
        arbeidsgivere.values.find { it.identifikator == identifikator }

    override fun finnAlleForIdentifikatorer(identifikatorer: Set<Arbeidsgiver.Identifikator>) =
        arbeidsgivere.values.filter { it.identifikator in identifikatorer }

    fun alle() = arbeidsgivere.values.toList()
}
