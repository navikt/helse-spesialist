package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling

interface SaksbehandlerhandlingRepository {

    fun lagre(saksbehandlerhandling: Saksbehandlerhandling)

}