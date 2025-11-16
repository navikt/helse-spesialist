package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import java.util.UUID

class InMemoryVarseldefinisjonRepository : VarseldefinisjonRepository,
    AbstractInMemoryRepository<VarseldefinisjonId, Varseldefinisjon>() {
    override fun finnGjeldendeFor(kode: String): Varseldefinisjon? = alle().find { it.kode == kode }

    fun lagre(kode: String) {
        lagre(
            Varseldefinisjon.fraLagring(
                id = VarseldefinisjonId(UUID.randomUUID()),
                kode = kode,
                tittel = "En tittel",
                forklaring = "En forklaring",
                handling = "En handling",
            )
        )
    }

    override fun tildelIder(root: Varseldefinisjon) {
        // ID er satt på forhånd, trenger aldri tildele en fra databasen
    }

    override fun deepCopy(original: Varseldefinisjon): Varseldefinisjon = Varseldefinisjon.fraLagring(
        id = original.id(),
        kode = original.kode,
        tittel = original.tittel,
        forklaring = original.forklaring,
        handling = original.handling,
    )
}
