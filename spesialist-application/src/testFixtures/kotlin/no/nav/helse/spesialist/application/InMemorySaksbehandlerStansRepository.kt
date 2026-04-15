package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansId

class InMemorySaksbehandlerStansRepository : AbstractInMemoryRepository<SaksbehandlerStansId, SaksbehandlerStans>(),
    SaksbehandlerStansRepository {
    override fun deepCopy(original: SaksbehandlerStans): SaksbehandlerStans =
        SaksbehandlerStans.fraLagring(
            id = original.id,
            identitetsnummer = original.identitetsnummer,
            utførtAv = original.utførtAv,
            begrunnelse = original.begrunnelse,
            opprettet = original.opprettet,
            stansOpphevet = original.stansOpphevet,
        )

    override fun finnAlle(identitetsnummer: Identitetsnummer): List<SaksbehandlerStans> =
        alle()
            .filter { it.identitetsnummer == identitetsnummer }
            .sortedByDescending { it.opprettet }

    override fun finnAktiv(identitetsnummer: Identitetsnummer): SaksbehandlerStans? =
        alle()
            .filter { it.identitetsnummer == identitetsnummer && it.erStanset }
            .maxByOrNull { it.opprettet }
}