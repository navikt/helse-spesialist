package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator

class InMemoryArbeidsgiverRepository : ArbeidsgiverRepository,
    AbstractInMemoryRepository<ArbeidsgiverIdentifikator, Arbeidsgiver>() {
    override fun tildelIder(root: Arbeidsgiver) {
        // ID er satt på forhånd, trenger aldri tildele en fra databasen
    }

    override fun deepCopy(original: Arbeidsgiver): Arbeidsgiver = Arbeidsgiver.Factory.fraLagring(
        id = original.id(),
        navn = original.navn,
    )
}
