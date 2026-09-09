package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId

interface VarseldefinisjonRepository {
    fun finnGjeldendeForOrNull(kode: String): Varseldefinisjon?

    fun finnOrNull(id: VarseldefinisjonId): Varseldefinisjon?

    fun lagre(varseldefinisjon: Varseldefinisjon)
}
