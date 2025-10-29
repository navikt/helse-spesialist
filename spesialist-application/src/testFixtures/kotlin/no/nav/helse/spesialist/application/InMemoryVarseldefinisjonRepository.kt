package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import java.util.UUID

class InMemoryVarseldefinisjonRepository: VarseldefinisjonRepository {
    private val definisjoner = mutableMapOf<String, Varseldefinisjon>()
    override fun finnGjeldendeFor(kode: String): Varseldefinisjon? {
        return definisjoner[kode]
    }

    fun lagre(kode: String) {
        definisjoner[kode] = Varseldefinisjon.fraLagring(
            id = VarseldefinisjonId(UUID.randomUUID()),
            kode = kode,
            tittel = "En tittel"
        )
    }
}
