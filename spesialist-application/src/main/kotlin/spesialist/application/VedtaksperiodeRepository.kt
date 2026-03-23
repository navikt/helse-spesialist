package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

interface VedtaksperiodeRepository {
    fun finn(vedtaksperiodeId: VedtaksperiodeId): Vedtaksperiode?

    fun lagre(vedtaksperiode: Vedtaksperiode)

    fun finnAlleIderForPerson(identitetsnummer: Identitetsnummer): Set<VedtaksperiodeId>
}
