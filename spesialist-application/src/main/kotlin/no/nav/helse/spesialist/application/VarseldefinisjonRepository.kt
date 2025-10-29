package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Varseldefinisjon

interface VarseldefinisjonRepository {
    fun finnGjeldendeFor(kode: String): Varseldefinisjon?
}
