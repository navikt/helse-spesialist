package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.VeilederStansId

class InMemoryVeilederStansRepository :
    AbstractInMemoryRepository<VeilederStansId, VeilederStans>(),
    VeilederStansRepository {

    override fun deepCopy(original: VeilederStans): VeilederStans =
        VeilederStans.fraLagring(
            id = original.id,
            identitetsnummer = original.identitetsnummer,
            årsaker = original.årsaker,
            opprettet = original.opprettet,
            originalMeldingId = original.originalMeldingId,
            stansOpphevet = original.stansOpphevet,
        )

    override fun finnAlle(identitetsnummer: Identitetsnummer): List<VeilederStans> =
        alle()
            .filter { it.identitetsnummer == identitetsnummer }
            .sortedByDescending { it.opprettet }

    override fun finnAktiv(identitetsnummer: Identitetsnummer): VeilederStans? =
        alle()
            .filter { it.identitetsnummer == identitetsnummer && it.erStansett }
            .maxByOrNull { it.opprettet }
}

