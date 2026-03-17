package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

class InMemorySaksbehandlerStansRepository : AbstractInMemoryRepository<Identitetsnummer, SaksbehandlerStans>(),
    SaksbehandlerStansRepository {
    override fun deepCopy(original: SaksbehandlerStans): SaksbehandlerStans =
        SaksbehandlerStans.fraLagring(original.events)
}