package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer

class InMemoryOpptegnelseRepository : OpptegnelseRepository,
    AbstractLateIdInMemoryRepository<Sekvensnummer, Opptegnelse>() {
    override fun tildelIder(root: Opptegnelse) {
        if (!root.harFåttTildeltId())
            root.tildelId(Sekvensnummer((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
    }

    override fun deepCopy(original: Opptegnelse): Opptegnelse = Opptegnelse.fraLagring(
        id = original.id(),
        identitetsnummer = original.identitetsnummer,
        type = original.type,
        payload = original.payload,
    )

    override fun finnAlleForPersonEtter(
        opptegnelseId: Sekvensnummer,
        personIdentitetsnummer: Identitetsnummer
    ): List<Opptegnelse> = alle().filter { it.id().value > opptegnelseId.value && it.identitetsnummer == personIdentitetsnummer }

    override fun finnNyesteSekvensnummer(): Sekvensnummer = alle().maxBy { it.id().value }.id()
}
