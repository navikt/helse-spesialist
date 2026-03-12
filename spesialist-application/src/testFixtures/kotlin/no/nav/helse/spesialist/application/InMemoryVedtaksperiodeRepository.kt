package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class InMemoryVedtaksperiodeRepository :
    AbstractInMemoryRepository<VedtaksperiodeId, Vedtaksperiode>(),
    VedtaksperiodeRepository {
    override fun finnAlleIderForPerson(identitetsnummer: Identitetsnummer): Set<VedtaksperiodeId> =
        alle().filter { it.identitetsnummer == identitetsnummer }.map { it.id }.toSet()

    override fun deepCopy(original: Vedtaksperiode): Vedtaksperiode =
        Vedtaksperiode(
            id = original.id,
            identitetsnummer = original.identitetsnummer,
            organisasjonsnummer = original.organisasjonsnummer,
            forkastet = original.forkastet,
        )
}
