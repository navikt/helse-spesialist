package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId

interface VarseldefinisjonRepository {
    fun finnGjeldendeFor(kode: String): Varseldefinisjon?

    fun finn(id: VarseldefinisjonId): Varseldefinisjon?
}
