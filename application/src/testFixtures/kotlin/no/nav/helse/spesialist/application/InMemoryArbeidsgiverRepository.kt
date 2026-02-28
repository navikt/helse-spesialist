package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator

class InMemoryArbeidsgiverRepository : ArbeidsgiverRepository,
    AbstractInMemoryRepository<ArbeidsgiverIdentifikator, Arbeidsgiver>() {
    override fun deepCopy(original: Arbeidsgiver): Arbeidsgiver = Arbeidsgiver.Factory.fraLagring(
        id = original.id,
        navn = original.navn,
    )
}
