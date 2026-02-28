package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId

class InMemoryVarseldefinisjonRepository : VarseldefinisjonRepository,
    AbstractInMemoryRepository<VarseldefinisjonId, Varseldefinisjon>() {
    override fun finnGjeldendeFor(kode: String): Varseldefinisjon? = alle().find { it.kode == kode }

    override fun deepCopy(original: Varseldefinisjon): Varseldefinisjon = Varseldefinisjon.fraLagring(
        id = original.id,
        kode = original.kode,
        tittel = original.tittel,
        forklaring = original.forklaring,
        handling = original.handling,
    )
}
