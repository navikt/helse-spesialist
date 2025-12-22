package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class InMemoryVedtaksperiodeRepository :
    AbstractInMemoryRepository<VedtaksperiodeId, Vedtaksperiode>(),
    VedtaksperiodeRepository {
    override fun deepCopy(original: Vedtaksperiode): Vedtaksperiode =
        Vedtaksperiode(
            id = original.id,
            fødselsnummer = original.fødselsnummer,
            organisasjonsnummer = original.organisasjonsnummer,
            forkastet = original.forkastet,
        )
}
