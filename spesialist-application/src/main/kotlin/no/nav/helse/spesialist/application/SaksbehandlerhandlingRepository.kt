package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling

interface SaksbehandlerhandlingRepository {
    fun lagre(saksbehandlerhandling: Saksbehandlerhandling)

    fun finnAlle(identitetsnummer: Identitetsnummer): List<Saksbehandlerhandling>
}
